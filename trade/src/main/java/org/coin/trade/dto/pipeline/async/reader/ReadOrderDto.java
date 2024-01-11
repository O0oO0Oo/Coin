package org.coin.trade.dto.pipeline.async.reader;

import org.coin.trade.pipeline.asyncloop.redis.CustomOrderLock;

import java.util.List;

public record ReadOrderDto(
        CustomOrderLock lock,
        List<OrderSortedSetDto> orderSortedSetDtoList
) {
    public static ReadOrderDto of(CustomOrderLock lock, List<OrderSortedSetDto> orderSortedSetDtoList){
        return new ReadOrderDto(lock, orderSortedSetDtoList);
    }
}
