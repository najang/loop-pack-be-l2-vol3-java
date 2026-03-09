package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.vo.Money;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Embedded
    @Getter(AccessLevel.NONE)
    @AttributeOverride(name = "value", column = @Column(name = "original_total_price", nullable = false))
    private Money originalTotalPrice;

    @Embedded
    @Getter(AccessLevel.NONE)
    @AttributeOverride(name = "value", column = @Column(name = "discount_amount", nullable = false))
    private Money discountAmount;

    @Embedded
    @Getter(AccessLevel.NONE)
    @AttributeOverride(name = "value", column = @Column(name = "final_total_price", nullable = false))
    private Money finalTotalPrice;

    @Column(name = "user_coupon_id")
    private Long userCouponId;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id", nullable = false)
    private List<OrderItem> items;

    protected Order() {
    }

    public Order(Long userId, List<OrderItem> items) {
        this(userId, items, null, 0);
    }

    public Order(Long userId, List<OrderItem> items, Long userCouponId, int discountAmount) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 필수입니다.");
        }
        if (items == null || items.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 항목은 비어있을 수 없습니다.");
        }
        int original = items.stream().mapToInt(i -> i.getUnitPrice() * i.getQuantity()).sum();
        this.userId = userId;
        this.status = OrderStatus.ORDERED;
        this.items = new ArrayList<>(items);
        this.originalTotalPrice = new Money(original);
        this.discountAmount = new Money(discountAmount);
        this.finalTotalPrice = new Money(original - discountAmount);
        this.userCouponId = userCouponId;
    }

    public int getOriginalTotalPrice() {
        return originalTotalPrice.getValue();
    }

    public int getDiscountAmount() {
        return discountAmount.getValue();
    }

    public int getFinalTotalPrice() {
        return finalTotalPrice.getValue();
    }

    public void changeStatus(OrderStatus status) {
        this.status = status;
    }

    public void cancel() {
        if (this.status == OrderStatus.CANCELLED) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 취소된 주문입니다.");
        }
        if (this.status == OrderStatus.DELIVERED) {
            throw new CoreException(ErrorType.BAD_REQUEST, "배송 완료된 주문은 취소할 수 없습니다.");
        }
        this.status = OrderStatus.CANCELLED;
    }
}
