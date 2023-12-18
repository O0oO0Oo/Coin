package org.coin.order.dto.request;

import jakarta.validation.constraints.NotNull;

public record AddMarketPriceOrderRequest(
        @NotNull(message = "암호화폐 id는 필수입니다.")
        Long cryptoId,
        @NotNull(message = "구매 수량은 필수입니다.")
        Double quantity
) {
}
