package com.loopers.domain.coupon;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * 선착순 쿠폰 발급 비동기 결과 (commerce-streamer: writable).
 */
@Getter
@Entity
@Table(name = "coupon_issue_results")
public class CouponIssueResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, unique = true, length = 36)
    private String requestId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "coupon_template_id", nullable = false)
    private Long couponTemplateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CouponIssueStatus status;

    protected CouponIssueResult() {
    }

    public CouponIssueResult(String requestId, Long userId, Long couponTemplateId) {
        this.requestId = requestId;
        this.userId = userId;
        this.couponTemplateId = couponTemplateId;
        this.status = CouponIssueStatus.PENDING;
    }

    public void markSuccess() {
        this.status = CouponIssueStatus.SUCCESS;
    }

    public void markSoldOut() {
        this.status = CouponIssueStatus.SOLD_OUT;
    }

    public void markDuplicate() {
        this.status = CouponIssueStatus.DUPLICATE;
    }
}
