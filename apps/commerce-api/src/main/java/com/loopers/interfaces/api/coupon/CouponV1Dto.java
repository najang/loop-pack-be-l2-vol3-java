package com.loopers.interfaces.api.coupon;

import com.loopers.application.coupon.CouponInfo;
import com.loopers.application.coupon.CouponIssueRequestInfo;
import com.loopers.application.coupon.CouponIssueResultInfo;
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

    public record CouponIssueRequestResponse(String requestId) {
        public static CouponIssueRequestResponse from(CouponIssueRequestInfo info) {
            return new CouponIssueRequestResponse(info.requestId());
        }
    }

    public record CouponIssueResultResponse(String requestId, Long couponTemplateId, String status) {
        public static CouponIssueResultResponse from(CouponIssueResultInfo info) {
            return new CouponIssueResultResponse(info.requestId(), info.couponTemplateId(), info.status());
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
