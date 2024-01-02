package org.coin.wallet.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.coin.trade.dto.pipeline.writer.ProcessedOrderDto;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class WalletBulkUpdateRepository {
    private final EntityManager em;
    private static final String WALLET_QUANTITY_UPDATE_SQL =
            "UPDATE Wallet w " +
                    "SET w.quantity = w.quantity + :amount " +
                    "WHERE w.id = :id";

    public int walletBulkUpdateOperation(List<ProcessedOrderDto> processedOrderDtoList) {
        return processedOrderDtoList.stream().map(dto -> {
                    return em.createQuery(WALLET_QUANTITY_UPDATE_SQL)
                            .setParameter("amount", dto.getAmount())
                            .setParameter("id", dto.getId())
                            .executeUpdate();
                })
                .reduce(0, Integer::sum);
    }
}