package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
@Entity
@Table(name = "coupon_templates")
public class CouponTemplate extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private CouponType type;

    @Column(name = "value", nullable = false)
    private int value;

    @Column(name = "min_order_amount")
    private Integer minOrderAmount;

    @Column(name = "expired_at", nullable = false)
    private ZonedDateTime expiredAt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "max_quantity")
    private Integer maxQuantity;

    @Column(name = "issued_count", nullable = false)
    private int issuedCount;

    protected CouponTemplate() {
    }

    public CouponTemplate(String name, CouponType type, int value, Integer minOrderAmount, ZonedDateTime expiredAt) {
        this(name, type, value, minOrderAmount, expiredAt, null);
    }

    public CouponTemplate(String name, CouponType type, int value, Integer minOrderAmount, ZonedDateTime expiredAt, Integer maxQuantity) {
        validate(name, type, value, expiredAt);
        this.name = name;
        this.type = type;
        this.value = value;
        this.minOrderAmount = minOrderAmount;
        this.expiredAt = expiredAt;
        this.isActive = true;
        this.maxQuantity = maxQuantity;
        this.issuedCount = 0;
    }

    public boolean isFcfs() {
        return maxQuantity != null;
    }

    public boolean canIssue() {
        return isActive && getDeletedAt() == null && expiredAt.isAfter(ZonedDateTime.now());
    }

    public void deactivate() {
        this.isActive = false;
    }

    public int calculateDiscount(int amount) {
        if (type == CouponType.FIXED) {
            return Math.min(value, amount);
        }
        return amount * value / 100;
    }

    public void update(String name, CouponType type, int value, Integer minOrderAmount, ZonedDateTime expiredAt) {
        validate(name, type, value, expiredAt);
        this.name = name;
        this.type = type;
        this.value = value;
        this.minOrderAmount = minOrderAmount;
        this.expiredAt = expiredAt;
    }

    private void validate(String name, CouponType type, int value, ZonedDateTime expiredAt) {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰명은 비어있을 수 없습니다.");
        }
        if (type == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 타입은 필수입니다.");
        }
        if (value <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 할인 값은 0보다 커야 합니다.");
        }
        if (expiredAt == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 유효 기간은 필수입니다.");
        }
    }
}
