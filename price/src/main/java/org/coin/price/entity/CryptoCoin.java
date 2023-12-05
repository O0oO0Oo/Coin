package org.coin.price.entity;

import lombok.Builder;
import lombok.Getter;


@Getter
public class CryptoCoin  {
    private String coinName;
    private Double price;
    private Long timestamp;

    @Builder
    public CryptoCoin(String coinName, Double price, Long timestamp) {
        this.coinName = coinName;
        this.price = price;
        this.timestamp = timestamp;
    }
}