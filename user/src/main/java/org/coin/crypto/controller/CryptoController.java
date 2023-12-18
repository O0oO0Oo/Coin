package org.coin.crypto.controller;

import lombok.RequiredArgsConstructor;
import org.coin.common.request.ApiVersion;
import org.coin.crypto.dto.response.FindCryptoResponse;
import org.coin.crypto.service.CryptoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cryptos")
@RequiredArgsConstructor
public class CryptoController {
    private final CryptoService cryptoService;

    @ApiVersion("1")
    @GetMapping
    public ResponseEntity<FindCryptoResponse> findCryptoV1(){
        return ResponseEntity.ok(cryptoService.findAllActiveCrypto());
    }

    @ApiVersion("1")
    @PutMapping("/{crypto_id}")
    public ResponseEntity<Void> updateCryptoStatusV1(@PathVariable("crypto_id") Long cryptoId) {
        cryptoService.updateCryptoActiveStatus(cryptoId);
        return ResponseEntity.noContent().build();
    }
}
