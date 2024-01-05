package org.coin.trade.dto.pipeline.reader;

import org.coin.trade.redis.CustomOrderLock;
import org.redisson.api.RLock;

import java.util.List;

public record ReadOrderDto(
        CustomOrderLock lock,
        List<OrderSortedSetDto> orderSortedSetDtoList
) {
    public static ReadOrderDto of(CustomOrderLock lock, List<OrderSortedSetDto> orderSortedSetDtoList){
        return new ReadOrderDto(lock, orderSortedSetDtoList);
    }
}
