package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.vo.Money;
import com.loopers.domain.vo.Quantity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
@Entity
@Table(name = "order_items")
public class OrderItem extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Embedded
    @Getter(AccessLevel.NONE)
    private Quantity quantity;

    @Embedded
    @Getter(AccessLevel.NONE)
    @AttributeOverride(name = "value", column = @Column(name = "unit_price", nullable = false))
    private Money unitPrice;

    protected OrderItem() {
    }

    public OrderItem(Long productId, int quantity, int unitPrice) {
        if (productId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 ID는 필수입니다.");
        }
        this.productId = productId;
        this.quantity = new Quantity(quantity);
        this.unitPrice = new Money(unitPrice);
    }

    public int getQuantity() {
        return quantity.getValue();
    }

    public int getUnitPrice() {
        return unitPrice.getValue();
    }
}
