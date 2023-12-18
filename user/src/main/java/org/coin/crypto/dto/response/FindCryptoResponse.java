package org.coin.crypto.dto.response;

import org.coin.crypto.entity.Crypto;

import java.util.List;

public record FindCryptoResponse(
    List<Crypto> cryptoList
) {
    public static FindCryptoResponse of(List<Crypto> cryptoList) {
        return new FindCryptoResponse(cryptoList);
    }
}
