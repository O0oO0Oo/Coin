package org.coin.order.dto.request;

import jakarta.validation.constraints.NotNull;
import org.coin.order.validation.ValidTotalOrderPrice;

@ValidTotalOrderPrice
public record AddOrderRequest(
        @NotNull(message = "암호화폐 id는 필수입니다.")
        Long cryptoId,
        @NotNull(message = "구매 수량은 필수입니다.")
        Double quantity,
        @NotNull(message = "구매 가격은 필수입니다.")
        Double price
) {
}