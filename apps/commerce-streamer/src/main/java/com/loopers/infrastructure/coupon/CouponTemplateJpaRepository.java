package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.CouponTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponTemplateJpaRepository extends JpaRepository<CouponTemplate, Long> {

    @Query("SELECT c.issuedCount FROM CouponTemplate c WHERE c.id = :id")
    int findIssuedCountById(@Param("id") Long id);
}
