package org.coin.trade.config;

import org.coin.price.dto.CryptoCoin;
import org.coin.trade.pipeline.eventloop.redis.OrderLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.*;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.AssertionErrors;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 삭제(master) -> 락(master) -> 읽기(slave) 과정에서 발생하는 데이터 불일치 문제점 테스트
 */
@ActiveProfiles("dev")
@SpringBootTest(classes = RedissonWriteToMasterReadFromReplicaConfiguration.class)
class ReplicationLagTest {

    @Autowired
    private RedissonClient redissonClient;
    private final String KEY = "test:set";
    private final int testSize = 1000;

    @BeforeEach
    void beforeEach() {
        RScoredSortedSet<Object> scoredSortedSet = redissonClient.getScoredSortedSet(KEY, StringCodec.INSTANCE);
        for (int i = 0; i < testSize; i++) {
            scoredSortedSet.add(i, "m:" + i);
        }
    }

    /**
     * 충분한 딜레이가 있다면 성공
     */
    @Test
    @DisplayName("Replication-lag - 10ms delay")
    void should_success_when_deleteAndCountWith10msDelay() throws ExecutionException, InterruptedException {
        // given
        RScoredSortedSet<Object> scoredSortedSet = redissonClient.getScoredSortedSet(KEY, StringCodec.INSTANCE);

        // when, then
        // 내가 구현한 코드는 서로 다른 스레드에서 동작하므로 delete 와 find 의 동작이 같은 스레드로 진행되면 안된다.
        for (int i = 0; i < testSize; i++) {
            // 삭제
            scoredSortedSet.removeAsync("m:" + i);

            // 락이 10ms 걸린다 가정 동기화에 충분한 시간이니 성공해야한다.
            Thread.sleep(10);

            // 갯수 카운트
            int size = scoredSortedSet.count(0, true, 10000000, true);
            if (size != (testSize - (i + 1))) {
                AssertionErrors.fail("replication lag occurred at " + ("m:" + i) + " !");
            }
        }
    }

    /**
     * 삭제, 1ms 이후 읽기를 시도하면 동기화에 충분한 시간이 아니기에 실패한다.
     * java.lang.AssertionError: replication lag occurred at m:64 !
     */
    @Test
    @DisplayName("Replication-lag - 1ms delay")
    void should_failure_when_deleteAndCountWith1msDelay() throws ExecutionException, InterruptedException {
        // given
        RScoredSortedSet<Object> scoredSortedSet = redissonClient.getScoredSortedSet(KEY, StringCodec.INSTANCE);

        // when, then
        // 내가 구현한 코드는 서로 다른 스레드에서 동작하므로 delete 와 find 의 동작이 같은 스레드로 진행되면 안된다.
        for (int i = 0; i < testSize; i++) {
            // 삭제
            scoredSortedSet.removeAsync("m:" + i);

            // 락이 1ms 걸린다 가정, 동기화에 충분하지 않은 시간
            Thread.sleep(1);

            // 갯수 카운트
            int size = scoredSortedSet.count(0, true, 10000000, true);

            if (size != (testSize - (i + 1))) {
                AssertionErrors.fail("replication lag occurred at " + ("m:" + i) + " !");
            }
        }
    }

    /**
     * 락 스크립트 작업에서도 간헐적으로 발생
     * java.lang.AssertionError: replication lag occurred at m:1491 !
     */
    @Test
    @DisplayName("Replication-lag - lock delay")
    void should_failure_when_deleteAndLockCountWithNoneDelay() throws ExecutionException, InterruptedException {
        RScoredSortedSet<Object> scoredSortedSet = redissonClient.getScoredSortedSet(KEY, StringCodec.INSTANCE);

        for (int i = 0; i < testSize; i++) {
            // given
            CryptoCoin btc = CryptoCoin.builder()
                    .timestamp(System.currentTimeMillis())
                    .coinName("BTC")
                    .price(ThreadLocalRandom.current().nextDouble())
                    .build();
            List<CryptoCoin> coins = List.of(btc);

            // when
            // 삭제
            scoredSortedSet.removeAsync("m:" + i);

            // 락 작업과 읽기
            OrderLock orderLock = new OrderLock(redissonClient, coins, 6000);
            Integer size = orderLock.tryLockAsync()
                    .thenApply(lock -> {
                        try {
                            return scoredSortedSet.countAsync(0, true, 10000000, true).get();
                        } catch (InterruptedException | ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                    }).get();

            // then
            if (size != (testSize - (i + 1))) {
                AssertionErrors.fail("replication lag occurred at " + ("m:" + i) + " !");
            }
        }
    }

    @Test
    @DisplayName("Replication-lag - 1ms delay - batch-sync 5ms")
    void should_failure_when_deleteAndCountWith1msDelayAndBatchSync() throws ExecutionException, InterruptedException {
        // given
        RScoredSortedSet<Object> scoredSortedSet = redissonClient.getScoredSortedSet(KEY, StringCodec.INSTANCE);
        BatchOptions sync = BatchOptions.defaults()
                .sync(1, Duration.ofMillis(5));

        for (int i = 0; i < testSize; i++) {
            // when
            // 삭제, 배치 작업으로 sync 옵션 설정
            RBatch batch = redissonClient.createBatch(sync);
            RScoredSortedSetAsync<Object> batchScoredSortedSet = batch.getScoredSortedSet(KEY);
            batchScoredSortedSet.removeAsync("m:" + i);
            batch.executeAsync();

            // 락이 1ms 걸린다 가정, 동기화에 충분하지 않은 시간
            Thread.sleep(1);

            // 갯수 카운트
            int size = scoredSortedSet.count(0, true, 10000000, true);

            // then
            if (size != (testSize - (i + 1))) {
                AssertionErrors.fail("replication lag occurred at " + ("m:" + i) + " !");
            }
        }
    }
}