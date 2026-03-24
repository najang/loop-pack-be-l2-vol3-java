package com.loopers.infrastructure.order;

import com.loopers.domain.order.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByIdAndDeletedAtIsNull(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id AND o.deletedAt IS NULL")
    Optional<Order> findByIdWithLock(@Param("id") Long id);
    List<Order> findByUserIdAndDeletedAtIsNull(Long userId);
    List<Order> findByUserIdAndCreatedAtBetweenAndDeletedAtIsNull(Long userId, ZonedDateTime startAt, ZonedDateTime endAt);
    List<Order> findByCreatedAtBetweenAndDeletedAtIsNull(ZonedDateTime startAt, ZonedDateTime endAt);
}
