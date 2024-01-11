package org.coin.trade.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.coin.trade.dto.service.OrderDto;
import org.coin.trade.pipeline.eventloop.redis.TradeLua;
import org.redisson.api.*;
import org.redisson.client.RedisException;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class TradeService {
    @Value("${redis.number-of-slaves}")
    private int numberOfSlaves;
    private final RedissonClient redissonClient;
    private final BatchOptions registerBatchOptions = BatchOptions.defaults();

    // TODO : 삭제 후 반영이 안되고 reader 에서 슬레이브로부터 읽기 시작하면 삭제된 데이터를 읽어 처리할 가능성이 있다. 문서를 보면 10milli 안에 된다고 하지만 실제 배포했을때 얼마나 걸릴지는 고려해봐야한다.
    private final BatchOptions deregisterBatchOptions = BatchOptions.defaults()
            .sync(numberOfSlaves, Duration.ofMillis(50));


    /**
     * 주문 등록
     * SortedSet
     * key = order:{type}:{coin name}:{price}
     * member = {orderId}:{walletId/userId}:{quantity}
     * score = timestamp
     * <p>
     * ex : order:buy:btc:100000 : buyOrderId:walletId:quantity
     *      order:sell:btc:100000 : sellOrderId:userId:quantity
     */
    public boolean registerOrder(OrderDto registerOrderDto) {
        RBatch batch = redissonClient.createBatch(registerBatchOptions);
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
        RBatch batch = redissonClient.createBatch(deregisterBatchOptions);
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
        keys.add("lock:order:" + deregisterOrderDto.coinName() + ":" + deregisterOrderDto.price());
        keys.add(deregisterOrderDto.history());
        keys.add(deregisterOrderDto.key());

        List<Object> values = new ArrayList<>();
        values.add(String.valueOf(deregisterOrderDto.timestamp()));
        values.add(deregisterOrderDto.member());

        return script.evalAsync(RScript.Mode.READ_WRITE, TradeLua.DEREGISTER_ORDER, RScript.ReturnType.BOOLEAN, keys, values.toArray(new Object[0]))
                .toCompletableFuture()
                .thenApply(result -> {
                    if (result instanceof Boolean res) {
                        return Boolean.TRUE.equals(res);
                    }
                    return false;
                });
    }
}