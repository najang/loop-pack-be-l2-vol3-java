package com.loopers.interfaces.api.coupon;

import com.loopers.application.coupon.CouponInfo;
import com.loopers.application.coupon.UserCouponInfo;

import java.time.ZonedDateTime;

public class CouponV1Dto {

    public record CouponResponse(
        Long id,
        String name,
        String type,
        int value,
        Integer minOrderAmount,
        ZonedDateTime expiredAt
    ) {
        public static CouponResponse from(CouponInfo info) {
            return new CouponResponse(
                info.id(),
                info.name(),
                info.type(),
                info.value(),
                info.minOrderAmount(),
                info.expiredAt()
            );
        }
    }

    public record UserCouponResponse(
        Long id,
        Long couponTemplateId,
        String status,
        ZonedDateTime usedAt,
        CouponResponse template
    ) {
        public static UserCouponResponse from(UserCouponInfo info) {
            return new UserCouponResponse(
                info.id(),
                info.couponTemplateId(),
                info.status(),
                info.usedAt(),
                CouponResponse.from(info.template())
            );
        }
    }
}
