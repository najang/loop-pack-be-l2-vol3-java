package com.loopers.application.coupon;

public record CouponIssueMessage(String requestId, Long userId, Long couponTemplateId, int maxQuantity) {
}
