package org.coin.trade.dto.pipeline.event.reader;

import org.coin.trade.dto.pipeline.event.lock.LockResultDto;

import java.util.Collection;

public record OrderSortedSetDto(
        String type,
        String coinName,
        double price,
        Collection<String> orders
) {
    public static OrderSortedSetDto of(String type, LockResultDto lockResultDto, Collection<?> orders) {
        // TODO : List<String> codec 고려해보기
        return new OrderSortedSetDto(type, lockResultDto.name(), lockResultDto.price(), (Collection<String>) orders);
    }

    public String key() {
        return "order:" + type + ":" + coinName + ":" + price;
    }
}

