package org.coin.price.service;

import lombok.RequiredArgsConstructor;
import org.coin.price.dto.CurrentPrice;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PriceService {
    private final CurrentPrice currentPrice;

    public String findPrice(String coinName) {
        return currentPrice.getCurrentPrice(coinName);
    }
}