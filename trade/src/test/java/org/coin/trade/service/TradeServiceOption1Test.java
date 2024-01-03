package org.coin.trade.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;

@ActiveProfiles("test")
@SpringBootTest
class TradeServiceOption1Test {
    @Autowired
    private RedissonClient redissonClient;
    private AtomicInteger lockCount = new AtomicInteger();

    private final int THREAD_N = 10;
    private final int TASK_N = 1000;


    @Test
    @DisplayName("trade module - deregister - lock")
    void should_complete_when_multiThreadTryLock() throws InterruptedException {
        // given
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_N);

        // when
        long start = System.currentTimeMillis();
        List<CompletableFuture<Void>> completableFutures = IntStream.range(0, TASK_N)
                .mapToObj(i -> CompletableFuture.supplyAsync(deregisterTryLock(), executor)
                        .thenAccept(this::countLock)
                ).toList();

        CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0])).join();
        long end = System.currentTimeMillis();
        // then
        Assertions.assertAll(
                "multiThread tryLock test.",
                () -> Assertions.assertTrue(10000 > (end - start), "1000 tasks should take less than 10 seconds."),
                () -> Assertions.assertEquals(TASK_N, lockCount.get(), "All tasks should be done.")
        );
    }

    /**
     * 주문 처리는 read/write 락 둘 다 보유해야함.
     * 주문 읽기는 read 가 없고 write 를 보유 가능한 상황에서 동작. 이미 다른 스레드가 앞서 말한 조건으로 획득했다면 삭제 진행
     * @return
     */
    private Supplier<Boolean> deregisterTryReadWriteLock() {
        return () -> {
            RReadWriteLock lock = redissonClient.getReadWriteLock("lock:test");
            boolean isLocked = false;
            try {
                if (lock.writeLock().isLocked() && !lock.readLock().isLocked()) {
                    return true;
                }

                isLocked = lock.writeLock().tryLock(10, 1, TimeUnit.SECONDS);
                if (isLocked) {
                    // do redis task
                    
                    return true;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                if (isLocked) {
                    lock.writeLock().unlock();
                }
            }
            return false;
        };
    }

    private Supplier<Boolean> deregisterTryLock() {
        return () -> {
            RLock lock = redissonClient.getLock("lock:test");
            boolean isLocked = false;
            try {
                // 이미 락을 가지고 있다면 삭제, 없다면 tryLock
                if (lock.isLocked() || lock.tryLock(10, 10, TimeUnit.SECONDS)) {
                    // do redis task

                    return true;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
            return false;
        };
    }

    private void countLock(Boolean result) {
        if(Boolean.TRUE.equals(result)){
            lockCount.incrementAndGet();
        }
    }
}