package org.coin.crypto.service;

import lombok.RequiredArgsConstructor;
import org.coin.common.exception.CustomException;
import org.coin.common.exception.ErrorCode;
import org.coin.crypto.dto.response.FindCryptoResponse;
import org.coin.crypto.entity.Crypto;
import org.coin.crypto.repository.CryptoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CryptoService {
    private final CryptoRepository cryptoRepository;

    @Transactional(readOnly = true)
    public FindCryptoResponse findAllActiveCrypto() {
        return FindCryptoResponse.of(cryptoRepository.findAll());
    }

    @Transactional
    public void updateCryptoActiveStatus(Long cryptoId) {
        Crypto crypto = findCryptoByIdOrElseThrow(cryptoId);

        crypto.reverseActive();

        cryptoRepository.save(crypto);
    }

    private Crypto findCryptoByIdOrElseThrow(Long cryptoId) {
        return cryptoRepository.findById(cryptoId)
                .orElseThrow(() -> new CustomException(ErrorCode.CRYPTO_NOT_FOUND));
    }
}
