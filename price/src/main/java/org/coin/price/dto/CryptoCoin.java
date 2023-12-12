package org.coin.price.dto;

import lombok.Builder;
import lombok.Getter;


@Getter
public class CryptoCoin  {
    private String coinName;
    private String price;
    private Long timestamp;

    @Builder
    public CryptoCoin(String coinName, String price, Long timestamp) {
        this.coinName = coinName;
        this.price = price;
        this.timestamp = timestamp;
    }
}