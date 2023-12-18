package org.coin.order.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.coin.common.request.ApiVersion;
import org.coin.order.dto.request.AddMarketPriceOrderRequest;
import org.coin.order.dto.request.AddOrderRequest;
import org.coin.order.dto.response.AddSellOrderResponse;
import org.coin.order.dto.response.FindOrderResponse;
import org.coin.order.service.SellOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/users/{user_id}")
@RequiredArgsConstructor
public class SellOrderController {
    private final SellOrderService sellOrderService;

    @ApiVersion("1")
    @PostMapping("/sell-orders")
    public ResponseEntity<AddSellOrderResponse> addSellOrderV1(@PathVariable("user_id") Long userId,
                                                           @Valid @RequestBody AddOrderRequest request
    ) {
        AddSellOrderResponse addSellOrderResponse = sellOrderService.executeSellOrderTransaction(userId, request);
        return ResponseEntity
                .created(URI.create("/api/users/" + userId + "/sell-orders/" + addSellOrderResponse.orderId()))
                .body(addSellOrderResponse);
    }

    @ApiVersion("1")
    @PostMapping("/market-price-sell-orders")
    public ResponseEntity<AddSellOrderResponse> addSellOrderByMarketPriceV1(@PathVariable("user_id") Long userId,
                                                                          @Valid @RequestBody AddMarketPriceOrderRequest request) {
        AddSellOrderResponse addSellOrderResponse = sellOrderService.executeMarketPriceSellOrderTransaction(userId, request);
        return ResponseEntity
                .created(URI.create("api/users/" + userId + "/sell-orders/" + addSellOrderResponse.orderId()))
                .body(addSellOrderResponse);
    }

    @ApiVersion("1")
    @GetMapping("/sell-orders")
    public ResponseEntity<FindOrderResponse> findSellOrderV1(@PathVariable("user_id") Long userId) {
        return ResponseEntity
                .ok(sellOrderService.findAllSellOrder(userId));
    }

    @ApiVersion("1")
    @DeleteMapping("/sell-orders/{sell_order_id}")
    public ResponseEntity<Void> deleteSellOrderV1(@PathVariable("user_id") Long userId,
                                         @PathVariable("sell_order_id") Long sellOrderId
    ) {
        sellOrderService.cancelAndRefundSellOrderTransaction(userId, sellOrderId);
        return ResponseEntity.noContent().build();
    }
}
