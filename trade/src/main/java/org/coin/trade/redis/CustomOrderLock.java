package org.coin.trade.redis;

import lombok.Getter;
import org.coin.price.dto.CryptoCoin;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.util.ArrayList;
import java.util.List;

public class CustomOrderLock {
    private final RedissonClient redissonClient;
    private final List<CryptoCoin> prices;
    @Getter
    private final String name;

    public CustomOrderLock(RedissonClient redissonClient, List<CryptoCoin> prices) {
        this.redissonClient = redissonClient;
        this.prices = prices;
        this.name = prices.get(0).getCoinName();
    }

    public boolean tryLock(int leaseTime) {
        List<Object> keys = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        keys.add("lock:" + name);
        values.add(String.valueOf(Thread.currentThread().getId()));
        values.add(leaseTime);
        prices
                .forEach(coin -> {
                    keys.add("lock:history:" + name + ":" + coin.getPrice());
                    values.add(String.valueOf(coin.getTimestamp()));
                });

        RScript script = redissonClient.getScript(StringCodec.INSTANCE);
        return script.eval(RScript.Mode.READ_WRITE, RedisScript.LOCK_WRITE_HISTORY, RScript.ReturnType.BOOLEAN, keys, values.toArray(new Object[0]));
    }

    public boolean unlock() {
        List<Object> keys = new ArrayList<>();

        keys.add("lock:" + name);
        prices
                .forEach(coin -> {
                    keys.add("lock:history:" + name + ":" + coin.getPrice());
                });

        RScript script = redissonClient.getScript(StringCodec.INSTANCE);
        return script.eval(RScript.Mode.READ_WRITE, RedisScript.UNLOCK_DELETE_HISTORY, RScript.ReturnType.BOOLEAN, keys);
    }

    public boolean isLocked() {
        return redissonClient.getSet("lock:" + name).isExists();
    }
}
