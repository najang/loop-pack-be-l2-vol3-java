package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.coupon.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class UserCouponRepositoryImpl implements UserCouponRepository {

    private final UserCouponJpaRepository userCouponJpaRepository;

    @Override
    public Optional<UserCoupon> findById(Long id) {
        return userCouponJpaRepository.findById(id);
    }

    @Override
    public List<UserCoupon> findByUserId(Long userId) {
        return userCouponJpaRepository.findByUserIdAndDeletedAtIsNull(userId);
    }

    @Override
    public Page<UserCoupon> findByCouponTemplateId(Long couponTemplateId, Pageable pageable) {
        return userCouponJpaRepository.findByCouponTemplateIdAndDeletedAtIsNull(couponTemplateId, pageable);
    }

    @Override
    public boolean existsByUserIdAndCouponTemplateId(Long userId, Long couponTemplateId) {
        return userCouponJpaRepository.existsByUserIdAndCouponTemplateIdAndDeletedAtIsNull(userId, couponTemplateId);
    }

    @Override
    public int useIfAvailable(Long id, ZonedDateTime usedAt) {
        return userCouponJpaRepository.useIfAvailable(id, usedAt);
    }

    @Override
    public UserCoupon save(UserCoupon userCoupon) {
        return userCouponJpaRepository.save(userCoupon);
    }
}
