package com.loopers.infrastructure.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;

    @Override
    public Optional<Order> findById(Long id) {
        return orderJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public Optional<Order> findByIdWithLock(Long id) {
        return orderJpaRepository.findByIdWithLock(id);
    }

    @Override
    public List<Order> findByUserId(Long userId) {
        return orderJpaRepository.findByUserIdAndDeletedAtIsNull(userId);
    }

    @Override
    public List<Order> findByUserIdAndPeriod(Long userId, ZonedDateTime startAt, ZonedDateTime endAt) {
        return orderJpaRepository.findByUserIdAndCreatedAtBetweenAndDeletedAtIsNull(userId, startAt, endAt);
    }

    @Override
    public List<Order> findAllByPeriod(ZonedDateTime startAt, ZonedDateTime endAt) {
        return orderJpaRepository.findByCreatedAtBetweenAndDeletedAtIsNull(startAt, endAt);
    }

    @Override
    public Order save(Order order) {
        return orderJpaRepository.save(order);
    }
}
