package org.coin.wallet.service;

import lombok.RequiredArgsConstructor;
import org.coin.wallet.dto.response.FindWalletResponse;
import org.coin.wallet.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletService {
    private final WalletRepository walletRepository;

    @Transactional(readOnly = true)
    public FindWalletResponse findAllWallet(Long userId) {
        return FindWalletResponse.of(walletRepository.findByUserId(userId));
    }
}
