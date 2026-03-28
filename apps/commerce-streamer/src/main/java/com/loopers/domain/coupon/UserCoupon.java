package com.loopers.domain.coupon;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

/**
 * 발급된 쿠폰 (commerce-streamer: save 전용 minimal 엔티티).
 */
@Getter
@Entity
@Table(
    name = "user_coupons",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_user_coupon_template",
        columnNames = {"user_id", "coupon_template_id"}
    )
)
public class UserCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "coupon_template_id", nullable = false)
    private Long couponTemplateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserCouponStatus status;

    protected UserCoupon() {
    }

    public UserCoupon(Long userId, Long couponTemplateId) {
        this.userId = userId;
        this.couponTemplateId = couponTemplateId;
        this.status = UserCouponStatus.AVAILABLE;
    }
}
