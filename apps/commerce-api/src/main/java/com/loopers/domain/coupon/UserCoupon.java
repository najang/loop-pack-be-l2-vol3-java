package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
@Entity
@Table(
    name = "user_coupons",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_user_coupon_template",
        columnNames = {"user_id", "coupon_template_id"}
    )
)
public class UserCoupon extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "coupon_template_id", nullable = false)
    private Long couponTemplateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserCouponStatus status;

    @Column(name = "used_at")
    private ZonedDateTime usedAt;

    protected UserCoupon() {
    }

    public UserCoupon(Long userId, Long couponTemplateId) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 필수입니다.");
        }
        if (couponTemplateId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 템플릿 ID는 필수입니다.");
        }
        this.userId = userId;
        this.couponTemplateId = couponTemplateId;
        this.status = UserCouponStatus.AVAILABLE;
    }

    public void canUse(int orderAmount, CouponTemplate template) {
        if (status != UserCouponStatus.AVAILABLE) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용 가능한 쿠폰이 아닙니다.");
        }
        if (template.getMinOrderAmount() != null && orderAmount < template.getMinOrderAmount()) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                "최소 주문 금액(" + template.getMinOrderAmount() + "원) 이상이어야 쿠폰을 사용할 수 있습니다.");
        }
    }

    public void use() {
        if (status != UserCouponStatus.AVAILABLE) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용 가능한 쿠폰이 아닙니다.");
        }
        this.status = UserCouponStatus.USED;
        this.usedAt = ZonedDateTime.now();
    }

    public void expire() {
        this.status = UserCouponStatus.EXPIRED;
    }
}
