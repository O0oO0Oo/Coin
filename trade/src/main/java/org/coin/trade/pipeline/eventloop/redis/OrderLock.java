package org.coin.trade.pipeline.eventloop.redis;

import lombok.extern.slf4j.Slf4j;
import org.coin.price.dto.CryptoCoin;
import org.coin.trade.dto.pipeline.event.lock.LockResultDto;
import org.redisson.api.RFuture;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

@Slf4j
public class OrderLock implements Lock{
    private RedissonClient redissonClient;
    private List<CryptoCoin> prices;
    private CompletableFuture<List<?>> lockFuture;
    private List<LockResultDto> lockResultDtoList = new ArrayList<>();
    private int leaseTime;

    public List<LockResultDto> getLockResultDtoList() {
        return lockResultDtoList;
    }

    public OrderLock(RedissonClient redissonClient, List<CryptoCoin> prices, int leaseTime) {
        this.redissonClient = redissonClient;
        this.prices = prices;
        this.leaseTime = leaseTime;
    }

    @Override
    public boolean tryLock() {
        List<Object> keys = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        // ThreadId
        values.add(String.valueOf(Thread.currentThread().getId()));
        values.add(leaseTime);

        prices.forEach(price -> {
            String key = "lock:order:" + price.getCoinName() + ":" + price.getPrice();
            keys.add(key);
            values.add(String.valueOf(price.getTimestamp()));
        });

        RScript script = redissonClient.getScript(StringCodec.INSTANCE);
        List<String> result = script.eval(RScript.Mode.READ_WRITE, TradeLua.LOCK_WRITE_HISTORY, RScript.ReturnType.MULTI, keys, values.toArray(new Object[0]));

        if(result.isEmpty()){
            return false;
        }
        result.forEach(res -> lockResultDtoList.add(LockResultDto.of(res)));
        return true;
    }

    /**
     * 비동기 락 리스트 획득
     * lock:order:BTC:6.3766E7:1704929698503:1704929698503 다음과 같이 획득한 락에 대해 주문을 검색 가능하다.
     * @return
     */
    public CompletableFuture<List<?>> tryLockAsync() {
        List<Object> keys = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        values.add(String.valueOf(Thread.currentThread().getId()));
        values.add(leaseTime);

        prices.forEach(price -> {
            String key = "lock:order:" + price.getCoinName() + ":" + price.getPrice();
            keys.add(key);
            values.add(String.valueOf(price.getTimestamp()));
        });

        // 다음과 같이 [lock:order:BTC:6.3766E7:1704929698503:1704929698503, lock:order:BTC:6.3755E7:1704929698259:1704929698259, lock:order:BTC:6.3759E7:1704929698018:1704929698018]
        RScript script = redissonClient.getScript(StringCodec.INSTANCE);
        lockFuture = script.evalAsync(RScript.Mode.READ_WRITE, TradeLua.LOCK_WRITE_HISTORY, RScript.ReturnType.MULTI, keys, values.toArray(new Object[0]))
                .thenApply(res -> {
                    // Lock 데이터 받아서 초기화
                    if (res instanceof List<?> resString && (!resString.isEmpty())) {
                        resString.forEach(lockString -> lockResultDtoList.add(LockResultDto.of((String) lockString)));
                        return resString;
                    }
                    return Collections.emptyList();
                }).toCompletableFuture();
        return lockFuture;
    }

    public RFuture<Boolean> unlockAsync() {
        List<Object> keys = new ArrayList<>();

        lockResultDtoList.forEach(dto -> keys.add(dto.lockKey()));

        RScript script = redissonClient.getScript(StringCodec.INSTANCE);
        return script.evalAsync(RScript.Mode.READ_WRITE, TradeLua.UNLOCK_DELETE_HISTORY, RScript.ReturnType.BOOLEAN, keys);
    }

    public CompletableFuture<List<?>> getLockFuture() {
        return lockFuture;
    }

    /**
     * Not Used
     */
    @Override
    public void unlock() {
    }

    /**
     * Not Used
     */
    @Override
    public void lock() {

    }

    /**
     * Not Used
     * @throws InterruptedException
     */
    @Override
    public void lockInterruptibly() throws InterruptedException {

    }

    /**
     * Not Used
     * @param time the maximum time to wait for the lock
     * @param unit the time unit of the {@code time} argument
     * @return
     * @throws InterruptedException
     */
    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;
    }

    /**
     * Not Used
     * @return
     */
    @Override
    public Condition newCondition() {
        return null;
    }
}
