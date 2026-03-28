package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCouponStreamerJpaRepository extends JpaRepository<UserCoupon, Long> {
}
