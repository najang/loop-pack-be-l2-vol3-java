package com.loopers.domain.cart;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.vo.Quantity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
@Entity
@Table(name = "carts")
public class Cart extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Embedded
    @Getter(AccessLevel.NONE)
    private Quantity quantity;

    protected Cart() {
    }

    public Cart(Long userId, Long productId, int quantity) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 필수입니다.");
        }
        if (productId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 ID는 필수입니다.");
        }
        this.userId = userId;
        this.productId = productId;
        this.quantity = new Quantity(quantity);
    }

    public int getQuantity() {
        return quantity.getValue();
    }

    public void addQuantity(int amount) {
        this.quantity = this.quantity.add(new Quantity(amount));
    }

    public void updateQuantity(int quantity) {
        this.quantity = new Quantity(quantity);
    }
}
