package org.coin.trade.dto.pipeline.reader;

import org.coin.price.dto.CryptoCoin;

import java.util.Collection;

public record OrderSortedSetDto(
        String type,
        String coinName,
        double price,
        Collection<String> orders
) {
    public static OrderSortedSetDto of(String type, CryptoCoin coin, Collection<?> orders) {
        // TODO : List<String> codec 고려해보기
        return new OrderSortedSetDto(type, coin.getCoinName(), coin.getPrice(), (Collection<String>) orders);
    }

    public String key() {
        return "order:" + type + ":" + coinName + ":" + price;
    }
}

