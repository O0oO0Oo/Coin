package org.coin.trade.dto.pipeline.event.reader;

import org.coin.trade.pipeline.eventloop.redis.OrderLock;

import java.util.List;

public record ReadOrderDto(
        OrderLock lock,
        List<OrderSortedSetDto> orderSortedSetDtoList
) {
    public static ReadOrderDto of(OrderLock orders, List<OrderSortedSetDto> orderSortedSetDtoList) {
        return new ReadOrderDto(orders, orderSortedSetDtoList);
    }
}