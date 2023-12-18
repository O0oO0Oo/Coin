package org.coin.order.repository;

import org.coin.order.dto.projection.FindOrderView;
import org.coin.order.entity.SellOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SellOrderRepository extends JpaRepository<SellOrder, Long> {

    List<FindOrderView> findSellOrdersByUserId(Long userId);
}
