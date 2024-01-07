package org.coin.user.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.coin.trade.dto.pipeline.async.writer.ProcessedOrderDto;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserBulkUpdateRepository {
    private final EntityManager em;

    private static final String USER_MONEY_UPDATE_SQL =
            "UPDATE User u " +
                    "SET u.money = u.money + :amount " +
                    "WHERE u.id = :id";

    public void userBulkUpdateOperation(List<ProcessedOrderDto> processedOrderDtoList) {
        processedOrderDtoList.stream().forEach(dto -> em.createQuery(USER_MONEY_UPDATE_SQL)
                .setParameter("amount", dto.getAmount())
                .setParameter("id", dto.getId())
                .executeUpdate());
    }
}
