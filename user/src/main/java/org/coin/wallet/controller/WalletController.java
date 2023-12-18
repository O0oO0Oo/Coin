package org.coin.wallet.controller;

import lombok.RequiredArgsConstructor;
import org.coin.common.request.ApiVersion;
import org.coin.wallet.dto.response.FindWalletResponse;
import org.coin.wallet.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{user_id}/wallets")
@RequiredArgsConstructor
public class WalletController {
    private final WalletService walletService;

    @ApiVersion("1")
    @GetMapping
    public ResponseEntity<FindWalletResponse> findWalletV1(@PathVariable("user_id") Long userId) {
        return ResponseEntity
                .ok(walletService.findAllWallet(userId));
    }
}