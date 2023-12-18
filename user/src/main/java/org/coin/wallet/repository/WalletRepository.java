package org.coin.wallet.repository;

import org.coin.wallet.dto.projection.FindWalletView;
import org.coin.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserIdAndCryptoId(Long userId, Long cryptoId);

    List<FindWalletView> findByUserId(Long userId);
}
