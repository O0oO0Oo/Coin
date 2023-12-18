package org.coin.order.dto.response;

import org.coin.order.dto.projection.FindOrderView;

import java.util.List;

public record FindOrderResponse(
        List<FindOrderView> orders
){
    public static FindOrderResponse of(List<FindOrderView> orderDtoList) {
        return new FindOrderResponse(orderDtoList);
    }
}
