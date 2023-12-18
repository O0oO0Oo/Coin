package org.coin.crypto.repository;

import org.coin.crypto.dto.projection.FindCryptoView;
import org.coin.crypto.entity.Crypto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CryptoRepository extends JpaRepository<Crypto, Long> {
    List<FindCryptoView> findAllByActiveIsTrue();
}
