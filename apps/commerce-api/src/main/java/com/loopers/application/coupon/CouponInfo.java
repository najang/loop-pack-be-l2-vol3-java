package com.loopers.application.coupon;

import com.loopers.domain.coupon.CouponTemplate;

import java.time.ZonedDateTime;

public record CouponInfo(
    Long id,
    String name,
    String type,
    int value,
    Integer minOrderAmount,
    ZonedDateTime expiredAt,
    boolean isActive,
    Integer maxQuantity,
    int issuedCount
) {
    public static CouponInfo from(CouponTemplate template) {
        return new CouponInfo(
            template.getId(),
            template.getName(),
            template.getType().name(),
            template.getValue(),
            template.getMinOrderAmount(),
            template.getExpiredAt(),
            template.isActive(),
            template.getMaxQuantity(),
            template.getIssuedCount()
        );
    }
}
