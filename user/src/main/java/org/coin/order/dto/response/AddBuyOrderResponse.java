package org.coin.order.dto.response;

import org.coin.order.entity.BuyOrder;

public record AddBuyOrderResponse(
        Long orderId,
        String cryptoName,
        Double quantity,
        Double money
) {
    public static AddBuyOrderResponse of(BuyOrder savedOrder, String cryptoName, Double money) {
        return new AddBuyOrderResponse(
                savedOrder.getId(),
                cryptoName,
                savedOrder.getQuantity(),
                money
        );
    }
}
