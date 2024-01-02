package org.coin.price.dto;

import lombok.Builder;
import lombok.Getter;


@Getter
public class CryptoCoin  {
    private String coinName;
    private double price;
    private Long timestamp;

    @Builder
    public CryptoCoin(String coinName, double price, Long timestamp) {
        this.coinName = coinName;
        this.price = price;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return coinName + ":" + price;
    }
}