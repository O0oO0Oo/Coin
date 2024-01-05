package org.coin.trade.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.coin.trade.dto.service.OrderDto;
import org.coin.trade.redis.RedisScript;
import org.redisson.api.*;
import org.redisson.client.RedisException;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class TradeService {
    private final RedissonClient redissonClient;
    private final BatchOptions batchOptions = BatchOptions.defaults();

    /**
     * 주문 등록
     * SortedSet
     * key = order:{type}:{coin name}:{price}
     * member = {orderId}:{walletId}:{userId}:{quantity}
     * score = timestamp
     * <p>
     * ex : order:buy:btc:100000 : buyOrderId:walletId:userId:quantity
     *      order:sell:btc:100000 : sellOrderId:walletId:userId:quantity
     */
    public boolean registerOrder(OrderDto registerOrderDto) {
        RBatch batch = redissonClient.createBatch(batchOptions);
        RScoredSortedSetAsync<String> orderScoredSortedSet = batch.getScoredSortedSet(registerOrderDto.key());
        orderScoredSortedSet.addAsync(registerOrderDto.timestamp(), registerOrderDto.member());
        try {
            batch.execute();
            log.info("Add-Batch execute.");
            return true;
        } catch (RedisException e) {
            batch.discard();
            log.error("Add-Batch discard.");
            throw new RedisException(e);
        }
    }

    public boolean deregisterOrder(OrderDto deregisterOrderDto) {
        RBatch batch = redissonClient.createBatch(batchOptions);
        RScriptAsync script = batch.getScript(StringCodec.INSTANCE);
        CompletableFuture<Boolean> result = evalCheckLockZrem(script, deregisterOrderDto);
        try {
            batch.execute();
            log.info("Delete-Batch execute.");
            return result.join();
        } catch (RedisException e) {
            batch.discard();
            log.error("Delete-Batch discard.");
            throw new RedisException(e);
        }
    }

    private CompletableFuture<Boolean> evalCheckLockZrem(RScriptAsync script, OrderDto deregisterOrderDto) {
        List<Object> keys = new ArrayList<>();
        keys.add("lock:" + deregisterOrderDto.coinName());
        keys.add(deregisterOrderDto.history());
        keys.add(deregisterOrderDto.key());

        List<Object> values = new ArrayList<>();
        values.add(String.valueOf(deregisterOrderDto.timestamp()));
        values.add(deregisterOrderDto.member());

        return script.evalAsync(RScript.Mode.READ_WRITE, RedisScript.CHECK_LOCK_ZREM, RScript.ReturnType.BOOLEAN, keys, values.toArray(new Object[0]))
                .toCompletableFuture()
                .thenApply(result -> {
                    if (result instanceof Boolean res) {
                        return Boolean.TRUE.equals(res);
                    }
                    return false;
                });
    }
}