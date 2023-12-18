package org.coin.order.dto.response;

import org.coin.order.entity.SellOrder;

public record AddSellOrderResponse(
        Long orderId,
        String cryptoName,
        Double walletQuantity
) {
    public static AddSellOrderResponse of(SellOrder saveOrder, String cryptoName, Double quantity) {
        return new AddSellOrderResponse(
                saveOrder.getId(), cryptoName, quantity
        );
    }
}
