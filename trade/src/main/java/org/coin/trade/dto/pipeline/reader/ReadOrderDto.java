package org.coin.trade.dto.pipeline.reader;

import org.redisson.api.RLock;

import java.util.List;

public record ReadOrderDto(
        RLock lock,
        List<OrderSortedSetDto> orderSortedSetDtoList
) {
    public static ReadOrderDto of(RLock lock, List<OrderSortedSetDto> orderSortedSetDtoList){
        return new ReadOrderDto(lock, orderSortedSetDtoList);
    }
}
