package com.loopers.application.cart;

import com.loopers.domain.cart.Cart;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.SellingStatus;

public record CartInfo(
    Long id,
    Long productId,
    String productName,
    int productPrice,
    SellingStatus sellingStatus,
    int quantity
) {
    public static CartInfo from(Cart cart, Product product) {
        return new CartInfo(
            cart.getId(),
            cart.getProductId(),
            product.getName(),
            product.getPrice(),
            product.getSellingStatus(),
            cart.getQuantity()
        );
    }
}
