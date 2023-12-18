package org.coin.wallet.dto.projection;

public interface FindWalletView {
    Long getId();

    String getCryptoId();
    String getCryptoName();

    Double getQuantity();
}
