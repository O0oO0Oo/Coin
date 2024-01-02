package org.coin.price.dto;

import lombok.Getter;
import org.coin.price.event.PriceMessageProduceEvent;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CurrentPrice {
    @Getter
    private Long timestamp = System.currentTimeMillis();
    private Map<String,PriceApiRequest.PriceData> currentPriceMap;

    public synchronized void setCurrentPrice(PriceMessageProduceEvent event) {
        if (event.timestamp().compareTo(this.timestamp) > 0) {
            this.timestamp = event.timestamp();
            this.currentPriceMap = event.priceDataMap();
        }
    }

    public double getCurrentPrice(String coinName) {
        return this.currentPriceMap.get(coinName).getClosing_price();
    }
}
