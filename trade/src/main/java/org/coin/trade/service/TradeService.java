package org.coin.trade.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.coin.trade.dto.service.OrderDto;
import org.redisson.api.BatchOptions;
import org.redisson.api.RBatch;
import org.redisson.api.RScoredSortedSetAsync;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.springframework.stereotype.Service;

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
}