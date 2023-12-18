package org.coin.wallet.dto.response;

import org.coin.wallet.dto.projection.FindWalletView;

import java.util.List;

public record FindWalletResponse(
        List<FindWalletView> walletList
) {

    public static FindWalletResponse of(List<FindWalletView> walletDtoList) {
        return new FindWalletResponse(walletDtoList);
    }
}
