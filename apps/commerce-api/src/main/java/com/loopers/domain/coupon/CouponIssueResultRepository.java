package com.loopers.domain.coupon;

import java.util.Optional;

public interface CouponIssueResultRepository {
    CouponIssueResult save(CouponIssueResult couponIssueResult);
    Optional<CouponIssueResult> findByRequestId(String requestId);
}
