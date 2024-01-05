package org.coin.trade.service;

import org.coin.price.dto.CryptoCoin;
import org.coin.trade.config.RedissonWriteToMasterReadFromReplicaConfiguration;
import org.coin.trade.dto.service.OrderDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@ActiveProfiles("test")
@SpringBootTest(classes = {RedissonWriteToMasterReadFromReplicaConfiguration.class, TradeService.class})
class TradeServiceOption2Test {
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private TradeService tradeService;

    /**
     * 락 이전 삭제, 성공
     */
    @Test
    @DisplayName("lua script - zrem -> tryLock -> read -> unlock ")
    void should_zrem_when_deleteBeforeLock() {
        // given
        long timestamp = System.currentTimeMillis();
        CryptoCoin coin = CryptoCoin.builder()
                .coinName("BTC")
                .timestamp(System.currentTimeMillis() - 1000)
                .price(5000).build();
        CryptoCoin coin1 = CryptoCoin.builder()
                .coinName("BTC")
                .timestamp(System.currentTimeMillis() - 500)
                .price(3000).build();
        CryptoCoin coin2 = CryptoCoin.builder()
                .coinName("BTC")
                .timestamp(System.currentTimeMillis())
                .price(2000).build();
        List<CryptoCoin> coins = List.of(coin, coin1, coin2);

        OrderDto orderDto = OrderDto.of("sell", "BTC", 3000, 1, 1, 100, timestamp);
        tradeService.registerOrder(orderDto);

        // when
        boolean zremResult = readHistoryZrem(orderDto);
        boolean lockResult = tryLockWriteHistory("BTC", 30000, coins);
        List<Object> zsetResult = redissonClient.getScoredSortedSet(orderDto.key())
                .valueRange(0, -1).stream().toList();
        boolean unLockResult = unLockDeleteHistory("BTC", coins);

        // then
        assertAll(
                "lua script test",
                () -> assertTrue(lockResult, "acquire lock, write history. result should be true"),
                () -> assertTrue(zremResult, "check lock, read history, delete member. result should be true"),
                () -> assertTrue(unLockResult, "unlock, delete history. result should be true"),
                () -> assertEquals(0, zsetResult.size(), "size should be 0.")
        );
    }

    /**
     * 락, 읽기 이후 삭제 실패
     */
    @Test
    @DisplayName("lua script - tryLock -> read -> zrem(already processing) -> unlock ")
    void should_failZrem_when_deleteDuringLock() {
        // given
        long timestamp = System.currentTimeMillis();
        CryptoCoin coin = CryptoCoin.builder()
                .coinName("BTC")
                .timestamp(timestamp - 500)
                .price(5000).build();
        CryptoCoin coin1 = CryptoCoin.builder()
                .coinName("BTC")
                .timestamp(timestamp - 250)
                .price(3000).build();
        CryptoCoin coin2 = CryptoCoin.builder()
                .coinName("BTC")
                .timestamp(timestamp)
                .price(2000).build();
        List<CryptoCoin> coins = List.of(coin, coin1, coin2);

        // ex: BTC, 10시 0분 1초 당시 2000원, 가격 등록은 10시 0분 0.9초 -> 따라서 현재 처리중인 가격이므로 취소주문은 거절되어야함.
        OrderDto orderDto = OrderDto.of("sell", "BTC", 2000, 1, 1, 100, timestamp - 100);
        tradeService.registerOrder(orderDto);

        // when
        boolean lockResult = tryLockWriteHistory("BTC", 30000, coins);
        List<Object> zsetResult = redissonClient.getScoredSortedSet(orderDto.key())
                .valueRange(0, -1).stream().toList();
        boolean zremResult = readHistoryZrem(orderDto);
        boolean unLockResult = unLockDeleteHistory("BTC", coins);

        // then
        assertAll(
                "lua script test",
                () -> assertTrue(lockResult, "acquire lock, write history. result should be true"),
                () -> assertFalse(zremResult, "check lock, read history, delete member. result should be false"),
                () -> assertTrue(unLockResult, "unlock, delete history. result should be true"),
                () -> assertEquals(1, zsetResult.size(), "size should be 0.")
        );
    }

    private boolean tryLockWriteHistory(String coinName, int milliseconds, List<CryptoCoin> coins) {
        List<Object> keys = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        keys.add("lock:" + coinName);
        values.add(String.valueOf(Thread.currentThread().getId()));
        values.add(milliseconds);
        coins
                .forEach(coin -> {
                    keys.add("lock:history:" + coinName + ":" + coin.getPrice());
                    values.add(String.valueOf(coin.getTimestamp()));
                });

        String tryLockHistoryScript = "" + //
                // lock / thread-id, PEXPIRE
                "local lock = redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2], 'NX') " +
                "if lock == false then " +
                // 락이 이미 있으면 false
                    "return false " +
                "end " +
                // 락이 없으면 기록, true 리턴
                // key = lock:history:BTC:4000.0 / timestamp = 1702020200
                "for i = 2, #KEYS do " +
                    "redis.call('HSET', KEYS[i], 'timestamp', ARGV[i+1]) " +
                    "redis.call('PEXPIRE', KEYS[i], ARGV[2]) " +
                "end " +
                "return true ";
        RScript script = redissonClient.getScript(StringCodec.INSTANCE);
        return script.eval(RScript.Mode.READ_WRITE, tryLockHistoryScript, RScript.ReturnType.BOOLEAN, keys, values.toArray(new Object[0]));
    }

    private boolean unLockDeleteHistory(String coinName, List<CryptoCoin> coins) {
        List<Object> keys = new ArrayList<>();

        keys.add("lock:" + coinName);
        coins
                .forEach(coin -> {
                    keys.add("lock:history:" + coinName + ":" + coin.getPrice());
                });

        String unLockHistory = "" +
                "for i = 1, #KEYS do " +
                    "redis.call('DEL', KEYS[i]) " +
                "end " +
                "return true ";

        RScript script = redissonClient.getScript(StringCodec.INSTANCE);
        return script.eval(RScript.Mode.READ_WRITE, unLockHistory, RScript.ReturnType.BOOLEAN, keys);
    }

    private boolean readHistoryZrem(OrderDto deregisterOrderDto) {
        List<Object> keys = new ArrayList<>();
        keys.add("lock:" + deregisterOrderDto.coinName());
        keys.add(deregisterOrderDto.history());
        keys.add(deregisterOrderDto.key());

        List<Object> values = new ArrayList<>();
        values.add(String.valueOf(deregisterOrderDto.timestamp()));
        values.add(deregisterOrderDto.member());

        String zremReadHistoryScript = "" +
                // 락 존재 확인
                "if redis.call('EXISTS', KEYS[1]) == 1 then " +
                    // 기록 존재 확인
                    "if redis.call('HGET', KEYS[2], 'timestamp') then " +
                        "if redis.call('HGET', KEYS[2], 'timestamp') <= ARGV[1] then " +
                            // ZREM 삭제
                            "redis.call('ZREM', KEYS[3], ARGV[2]) " +
                            "return true " +
                        "end " +
                    "end " +
                    "return false " +
                "end " +
                "redis.call('ZREM', KEYS[3], ARGV[2]) " +
                "return true ";
        RScript script = redissonClient.getScript(StringCodec.INSTANCE);
        return script.eval(RScript.Mode.READ_WRITE, zremReadHistoryScript, RScript.ReturnType.BOOLEAN, keys, values.toArray(new Object[0]));
    }
}