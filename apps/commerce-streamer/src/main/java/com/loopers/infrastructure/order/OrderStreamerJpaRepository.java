package com.loopers.infrastructure.order;

import com.loopers.domain.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderStreamerJpaRepository extends JpaRepository<Order, Long> {

    @Modifying
    @Query(value = "UPDATE orders SET status = :status WHERE id = :orderId", nativeQuery = true)
    int updateOrderStatus(@Param("orderId") Long orderId, @Param("status") String status);
}
