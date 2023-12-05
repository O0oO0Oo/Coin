package org.coin.price.event;

import org.coin.price.dto.PriceApiRequest;

import java.util.Map;

public record PriceMessageProduceEvent(
        Long timestamp,
        Map<String, PriceApiRequest.PriceData> priceDataMap
) {
    public static PriceMessageProduceEvent of(PriceApiRequest priceApiRequest) {
        return new PriceMessageProduceEvent(
                priceApiRequest.getData().getTimestamp(),
                priceApiRequest.getData().getPriceDataMap()
        );
    }
}