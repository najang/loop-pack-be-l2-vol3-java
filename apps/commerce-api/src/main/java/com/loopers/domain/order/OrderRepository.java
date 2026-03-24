package com.loopers.domain.order;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Optional<Order> findById(Long id);
    Optional<Order> findByIdWithLock(Long id);
    List<Order> findByUserId(Long userId);
    List<Order> findByUserIdAndPeriod(Long userId, ZonedDateTime startAt, ZonedDateTime endAt);
    List<Order> findAllByPeriod(ZonedDateTime startAt, ZonedDateTime endAt);
    Order save(Order order);
}
