package com.loopers.domain.coupon;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface UserCouponRepository {

    Optional<UserCoupon> findById(Long id);

    List<UserCoupon> findByUserId(Long userId);

    Page<UserCoupon> findByCouponTemplateId(Long couponTemplateId, Pageable pageable);

    boolean existsByUserIdAndCouponTemplateId(Long userId, Long couponTemplateId);

    int useIfAvailable(Long id, ZonedDateTime usedAt);

    UserCoupon save(UserCoupon userCoupon);
}
