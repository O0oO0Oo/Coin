package org.coin.trade.dto.pipeline.event.reader;

import org.coin.trade.dto.pipeline.async.reader.OrderSortedSetDto;
import org.coin.trade.redis.CustomOrderLock;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public record ReadOrderDto(
        CustomOrderLock lock,
        List<CompletableFuture<OrderSortedSetDto>> orderSortedSetDtoListCFList
) {
    public static ReadOrderDto of(CustomOrderLock lock, List<CompletableFuture<OrderSortedSetDto>> orderSortedSetDtoListCFList){
        return new ReadOrderDto(lock, orderSortedSetDtoListCFList);
    }
}
