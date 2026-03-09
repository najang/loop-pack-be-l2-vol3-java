package com.loopers.application.coupon;

import com.loopers.domain.coupon.CouponTemplate;
import com.loopers.domain.coupon.UserCoupon;

import java.time.ZonedDateTime;

public record UserCouponInfo(
    Long id,
    Long userId,
    Long couponTemplateId,
    String status,
    ZonedDateTime usedAt,
    CouponInfo template
) {
    public static UserCouponInfo from(UserCoupon userCoupon, CouponTemplate template) {
        return new UserCouponInfo(
            userCoupon.getId(),
            userCoupon.getUserId(),
            userCoupon.getCouponTemplateId(),
            userCoupon.getStatus().name(),
            userCoupon.getUsedAt(),
            CouponInfo.from(template)
        );
    }
}
