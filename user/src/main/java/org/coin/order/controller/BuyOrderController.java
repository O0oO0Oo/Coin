package org.coin.order.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.coin.common.request.ApiVersion;
import org.coin.order.dto.request.AddMarketPriceOrderRequest;
import org.coin.order.dto.request.AddOrderRequest;
import org.coin.order.dto.response.AddBuyOrderResponse;
import org.coin.order.dto.response.FindOrderResponse;
import org.coin.order.service.BuyOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/users/{user_id}")
@RequiredArgsConstructor
public class BuyOrderController {
    private final BuyOrderService buyOrderService;

    @ApiVersion("1")
    @PostMapping("/buy-orders")
    public ResponseEntity<AddBuyOrderResponse> addBuyOrderV1(@PathVariable("user_id") Long userId,
                                                           @Valid @RequestBody AddOrderRequest request
    ) {
        AddBuyOrderResponse addBuyOrderResponse = buyOrderService.executeBuyOrderTransaction(userId, request);
        return ResponseEntity
                .created(URI.create("/api/users/" + userId + "/buy-orders/" + addBuyOrderResponse.orderId()))
                .body(addBuyOrderResponse);
    }

    @ApiVersion("1")
    @PostMapping("/market-price-buy-orders")
    public ResponseEntity<AddBuyOrderResponse> addBuyOrderByMarketPriceV1(@PathVariable("user_id") Long userId,
                                                                        @Valid @RequestBody AddMarketPriceOrderRequest request) {
        AddBuyOrderResponse addBuyOrderResponse = buyOrderService.executeMarketPriceBuyOrderTransaction(userId, request);
        return ResponseEntity
                .created(URI.create("api/users/" + userId + "/buy-orders/" + addBuyOrderResponse.orderId()))
                .body(addBuyOrderResponse);
    }

    @ApiVersion("1")
    @GetMapping("/buy-orders")
    public ResponseEntity<FindOrderResponse> findBuyOrderV1(@PathVariable("user_id") Long userId) {
        return ResponseEntity
                .ok(buyOrderService.findAllBuyOrder(userId));
    }

    @ApiVersion("1")
    @DeleteMapping("/buy-orders/{buy_order_id}")
    public ResponseEntity<Void> deleteBuyOrderV1(@PathVariable("user_id") Long userId,
                                                 @PathVariable("buy_order_id") Long buyOrderId
                                         ) {
        buyOrderService.cancelAndRefundBuyOrderTransaction(userId, buyOrderId);
        return ResponseEntity.noContent().build();
    }
}