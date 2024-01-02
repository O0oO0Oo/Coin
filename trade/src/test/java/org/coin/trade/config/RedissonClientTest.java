package org.coin.trade.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.*;
import org.redisson.client.RedisException;
import org.redisson.transaction.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 구현하기 앞서 Redisson 이 요구사항대로 동작하는지 테스트
 * <P>
 * 현재 프로젝트에서 사용해야하는 기능
 * 1. 같은 주문이 여러번 처리되지 않도록 lock 기능
 * 2. 각 작업의 직렬가능
 *     - 주문 처리 작업 { 주문 데이터 읽기(lock) - 주문 데이터 처리 - 처리된 주문 삭제(unlock) }
 *     - 주문 추가 작업 { 주문 추가 }
 *       처리 작업중 추가 작업을 하였다고하여 처리되지않은 주문이 삭제되지 말아야함.
 *       즉 각 기능들이 직렬 가능 스케줄이어야함.
 * 3. 문제 발생시 롤백 등 취소기능
 */
@ActiveProfiles("test")
@SpringBootTest(classes = {RedissonWriteToMasterReadFromReplicaConfiguration.class})
class RedissonClientTest {
    @Autowired
    private RedissonClient redissonClient;
    private static final Logger logger = LoggerFactory.getLogger(RedissonWriteToMasterReadFromReplicaConfiguration.class);

    /**
     * 여러 스레드가 동시 접근할 때, 하나의 스레드만 락을 획득한다.
     */
    @Test
    @DisplayName("redisson - multithreading lock test")
    void should_onlyOneThreadAcquireLock_when_multiThreadAttemptToAcquireLock() throws InterruptedException, ExecutionException {
        // given
        final int THREAD_POOL_SIZE = 3;
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        AtomicInteger failedAcquireLockCount = new AtomicInteger();

        Runnable lockTestRunnable = () -> {
            RLock lock = redissonClient.getLock("lock");
            boolean lockResult = false;
            try {
                // acquire lock, wait 0, lock expire 1s
                lockResult = lock.tryLock(0, 1000L, TimeUnit.MILLISECONDS);
                if (lockResult) {
                    logger.info("acquire lock.");
                    // do some redis work

                } else {
                    failedAcquireLockCount.incrementAndGet();
                    logger.info("Failed to acquire lock.");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                if (lockResult){
                    logger.info("release lock.");
                    lock.unlock();
                }
            }
        };

        // when
        IntStream.range(0, THREAD_POOL_SIZE).forEach(i -> executorService.submit(lockTestRunnable));

        executorService.shutdown();
        executorService.awaitTermination(2000L, TimeUnit.MILLISECONDS);

        // then
        assertEquals(THREAD_POOL_SIZE - 1, failedAcquireLockCount.get());
    }

    /**
     * 멀티스레드 트랜잭션 테스트 - 부적합
     * 트랜잭션에는 ScoredSortedSet 이 없기 때문에, Bucket 을 사용하였다.
     * Bucket 에 Set 의 특성은 없지만 직렬가능한지에 대한 테스트이기에 먼저 PriorityQueue 를 사용, 저장 시 다음과 같이 저장
     * "[\"java.util.PriorityQueue\",[0,8]]"
     * <p>
     *     동작 예시
     * 처음 10
     * 스레드1 ; 10 -> 1개 삭제 -> 9개 남음
     * 스레드2 : 10 -> 1개 삭제 -> 9개 남음
     * ...
     * 마지막 실행 스레드10 : n -> 1개 삭제 -> n-1 개 남음
     * <p>
     * 결과 : 마지막 결과가 0이 아니다, 덮어씌어진다.
     */
    @Test
    @DisplayName("redisson - multithreading transaction - should serializable")
    void should_serializable_when_multiThreadTransaction() throws InterruptedException {
        // given`
        final int THREAD_POOL_SIZE = 5;
        String keyName = "bucket";
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE * 2);
        AtomicInteger countAddTransaction = new AtomicInteger();

        // initial size == THREAD_POOL_SIZE
        PriorityQueue<Integer> initialQueue = new PriorityQueue<>();
        RBucket<PriorityQueue<Integer>> initialBucket = redissonClient.getBucket(keyName);
        initialQueue.addAll(
                IntStream.range(0, THREAD_POOL_SIZE).boxed().toList()
        );
        initialBucket.set(initialQueue);

        TransactionOptions transactionOptions = TransactionOptions.defaults()
                .syncSlaves(1, 5000, TimeUnit.MILLISECONDS);

        // when
        CompletableFuture<Void> result = IntStream.range(0, THREAD_POOL_SIZE).mapToObj(i -> {
            CompletableFuture<Boolean> addCompletableFuture = CompletableFuture.supplyAsync(getTransactionAddSupplier(keyName, transactionOptions), executorService)
                    .whenComplete((res, throwable) -> {
                        if (res) {
                            countAddTransaction.incrementAndGet();
                        }
                    });
            CompletableFuture<Void> readCompletableFuture = CompletableFuture.runAsync(getTransactionReadAndDeleteRunnable(keyName, transactionOptions));
            return CompletableFuture.allOf(addCompletableFuture, readCompletableFuture);
        }).reduce(CompletableFuture.runAsync(() -> {}, CompletableFuture.delayedExecutor(1000, TimeUnit.MILLISECONDS)),
                CompletableFuture::allOf);

        result.join();

        // then
        RBucket<PriorityQueue<Integer>> resultBucket = redissonClient.getBucket(keyName);
        int size = resultBucket.get().size();
        resultBucket.delete();
        assertAll(
                "Grouped Assertions of Transaction",
                () -> assertEquals(THREAD_POOL_SIZE, countAddTransaction.get(), "Add-Transaction must executed " + THREAD_POOL_SIZE + " times"),
                () -> assertEquals(THREAD_POOL_SIZE, size, "Remained queue size should be " + THREAD_POOL_SIZE + ".")
        );
    }

    private Runnable getTransactionReadAndDeleteRunnable(String keyName, TransactionOptions transactionOptions) {
        return () -> {
            RLock lock = redissonClient.getLock(keyName + "lock");
            boolean lockResult = false;

            try {
                PriorityQueue<Integer> queue = null;
                // read & lock
                lockResult = lock.tryLock(1000, 5000, TimeUnit.MILLISECONDS);
                if (lockResult) {
                    RBucket<PriorityQueue<Integer>> bucket = redissonClient.getBucket(keyName);
                    queue = bucket.get();
                }

                // do some process

                // delete transaction
                logger.info("delete transaction start.");
                RTransaction transaction = redissonClient.createTransaction(transactionOptions);
                RBucket<PriorityQueue<Integer>> transactionBucket = transaction.getBucket(keyName);
                Objects.requireNonNull(queue).poll();
                transactionBucket.set(queue);

                try {
                    transaction.commit();
                    logger.info("delete transaction commit.");
                } catch (TransactionException e) {
                    transaction.rollback();
                    logger.error("delete transaction rollback.");
                }

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                if(lockResult) {
                    lock.unlock();
                    logger.info("release lock.");
                }
            }
        };
    }

    private Supplier<Boolean> getTransactionAddSupplier(String keyName, TransactionOptions transactionOptions) {
        return () -> {
            RTransaction transaction = redissonClient.createTransaction(transactionOptions);
            logger.info("add transaction start.");
            RBucket<PriorityQueue<Integer>> bucket = transaction.getBucket(keyName);
            PriorityQueue<Integer> queue = bucket.get();
            queue.add(ThreadLocalRandom.current().nextInt());
            bucket.set(queue);
            try {
                transaction.commit();
                logger.info("add transaction commit.");
            } catch (TransactionException e) {
                transaction.rollback();
                logger.error("add transaction rollback.");
                return false;
            }
            return true;
        };
    }

    /**
     * 멀티스레드 배치 테스트 - 성공
     * TODO : 최적화가 필요함.
     *        현재 처리된 데이터를 삭제하기 위해 사용중인
     *        ZREM key [member...] 의 경우 O(M*long(N)) 이다. (N 멤버의 갯수, M 삭제될 갯수)
     *        ZREMRANGEBYSCORE key min max 의 경우 O(log(N) + M) (위와 동일) 이다.
     *        ZREMRANGEBYSCORE 와 ZREM 간의 시간복잡도는 크게 차이가 난다. ZREMRANGEBYSCORE 을 사용할 수도 있지만
     *        ZREMRANGEBYSCORE 사용에는 다음과 같은 문제가 있다.
     *        현재 RedissonWriteToMasterReadFromReplicaConfiguration 이름 처럼 master 에 쓰고 slave 에서 읽고있다.
     *        Score(시간) 10시 0분 1초까지의 100원 가격 주문의 처리와, 10시 0분 0.09초의 100원 가격 주문이 등록이 동시에 발생했을 때.
     *        slave 와 master 간 동기화 지연이 발생하게 된다면, 해당 주문은 처리되어야 함에도 불구하고 처리가 안될수도 있다는 문제점이 발생한다.
     */
    @Test
    @DisplayName("redisson - multithreading batch - should serializable")
    void should_serializable_when_multiThreadBatch() throws InterruptedException {
        // given
        final int THREAD_POOL_SIZE = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        String keyName = "batch";
        AtomicInteger score = new AtomicInteger();

        RScoredSortedSet<String> initialZset = redissonClient.getScoredSortedSet(keyName);
        IntStream.range(0, THREAD_POOL_SIZE).forEach(i -> initialZset.add(i, "mem" + i));

        // batch option
        BatchOptions batchOptions = BatchOptions.defaults()
                .sync(1, Duration.ofMillis(5000));

        // when
        CompletableFuture<Void> result = IntStream.range(0, THREAD_POOL_SIZE).mapToObj(i -> {
                    CompletableFuture<Void> readBatch = CompletableFuture.runAsync(getBatchReadAndDeleteRunnable(keyName, batchOptions), executorService);
                    CompletableFuture<Boolean> addBatch = CompletableFuture.supplyAsync(getBatchAddSupplier(keyName, batchOptions, score), executorService);
                    return CompletableFuture.allOf(readBatch, addBatch);
                })
                .reduce(CompletableFuture.runAsync(() -> {
                        }, CompletableFuture.delayedExecutor(1000, TimeUnit.MILLISECONDS)),
                        CompletableFuture::allOf);
        result.join();

        // then
        RScoredSortedSet<String> resultZset = redissonClient.getScoredSortedSet(keyName);
        int size = resultZset.size();
        resultZset.delete();
        // 초기 사이즈 5 - 5번의 추가 5번의 삭제
        assertAll(
                "Grouped Assertions of Batch",
                () -> assertEquals(THREAD_POOL_SIZE, score.get(), "Add-Batch must be executed " + THREAD_POOL_SIZE + " times"),
                () -> assertEquals(THREAD_POOL_SIZE, size, "Remained queue size should be " + THREAD_POOL_SIZE)
        );
    }

    private Runnable getBatchReadAndDeleteRunnable(String keyName, BatchOptions batchOptions) {
        return () -> {
            RLock lock = redissonClient.getLock(keyName + "lock");
            boolean lockResult = false;
            try {
                List<String> members = null;
                // read & lock
                lockResult = lock.tryLock(1000, 5000, TimeUnit.MILLISECONDS);
                if (lockResult) {
                    RScoredSortedSet<String> zset = redissonClient.getScoredSortedSet(keyName);
                    members = zset.valueRange(0, 1).stream().toList();
                }

                // do some process

                // delete batch
                RBatch batch = redissonClient.createBatch(batchOptions);
                logger.info("delete batch start.");
                batch.getScoredSortedSet(keyName)
                        .removeAsync(Objects.requireNonNull(members).get(0));

                try {
                    batch.execute();
                    logger.info("delete batch execute.");
                } catch (RedisException e) {
                    batch.discard();
                    logger.error("delete batch discard.");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                if(lockResult) {
                    lock.unlock();
                    logger.info("release lock.");
                }
            }
        };
    }

    private Supplier<Boolean> getBatchAddSupplier(String keyName, BatchOptions batchOptions, AtomicInteger scoreAtomicInteger) {
        return () -> {
            RBatch batch = redissonClient.createBatch(batchOptions);
            logger.info("add batch start.");

            RScoredSortedSetAsync<String> zset = batch.getScoredSortedSet(keyName);
            int score = scoreAtomicInteger.incrementAndGet();
            zset.addAsync(score, "mem" + score + 10);

            try {
                batch.execute();
                logger.info("add batch execute.");
            } catch (RedisException e) {
                batch.discard();
                logger.error("add batch discard.");
            }
            return true;
        };
    }
}