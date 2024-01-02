package org.coin.order.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderBulkUpdateRepository {
    private final EntityManager em;
    private static final String PROCESSED_BUY_ORDER_UPDATE_SQL =
            "UPDATE BuyOrder b " +
            "SET b.processed = true " +
            "WHERE b.id in :ids";
    private static final String PROCESSED_SELL_ORDER_UPDATE_SQL =
            "UPDATE SellOrder s " +
            "SET s.processed = true " +
            "WHERE s.id in :ids";

    public int buyOrderBulkUpdateOperation(List<Long> ids) {
        return em.createQuery(PROCESSED_BUY_ORDER_UPDATE_SQL)
                .setParameter("ids", ids)
                .executeUpdate();
    }

    public int sellOrderBulkUpdateOperation(List<Long> ids) {
        return em.createQuery(PROCESSED_SELL_ORDER_UPDATE_SQL)
                .setParameter("ids", ids)
                .executeUpdate();
    }
}