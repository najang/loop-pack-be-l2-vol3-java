package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.CouponIssueResult;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface CouponTemplateStreamerJpaRepository extends Repository<CouponIssueResult, Long> {

    @Modifying
    @Query(value = "UPDATE coupon_templates SET issued_count = issued_count + 1 " +
        "WHERE id = :id AND issued_count < :maxQuantity AND deleted_at IS NULL", nativeQuery = true)
    int incrementIssuedCount(@Param("id") Long id, @Param("maxQuantity") int maxQuantity);

    @Modifying
    @Query(value = "UPDATE coupon_templates SET issued_count = issued_count - 1 " +
        "WHERE id = :id AND issued_count > 0", nativeQuery = true)
    int decrementIssuedCount(@Param("id") Long id);
}
