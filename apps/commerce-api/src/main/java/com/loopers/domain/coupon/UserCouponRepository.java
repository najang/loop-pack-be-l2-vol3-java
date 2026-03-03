package com.loopers.domain.coupon;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserCouponRepository {

    Optional<UserCoupon> findById(Long id);

    List<UserCoupon> findByUserId(Long userId);

    Page<UserCoupon> findByCouponTemplateId(Long couponTemplateId, Pageable pageable);

    UserCoupon save(UserCoupon userCoupon);
}
