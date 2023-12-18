package org.coin.order.repository;

import org.coin.order.dto.projection.FindOrderView;
import org.coin.order.entity.BuyOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BuyOrderRepository extends JpaRepository<BuyOrder, Long> {
    List<FindOrderView> findBuyOrdersByUserId(Long userId);
}
