package com.loopers.domain.coupon;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * 쿠폰 템플릿 최소 엔티티 (commerce-streamer: DDL 생성 + issued_count 조회용).
 * 쓰기는 CouponTemplateStreamerJpaRepository의 native query를 통해 수행한다.
 */
@Getter
@Entity
@Table(name = "coupon_templates")
public class CouponTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "coupon_type", nullable = false, length = 20)
    private String couponType;

    @Column(name = "discount_amount", nullable = false)
    private int discountAmount;

    @Column(name = "max_quantity", nullable = false)
    private int maxQuantity;

    @Column(name = "issued_count", nullable = false)
    private int issuedCount;

    @Column(name = "deleted_at")
    private java.time.ZonedDateTime deletedAt;

    protected CouponTemplate() {
    }

    public CouponTemplate(String name, int maxQuantity) {
        this.name = name;
        this.couponType = "FIXED";
        this.discountAmount = 0;
        this.maxQuantity = maxQuantity;
        this.issuedCount = 0;
    }
}
