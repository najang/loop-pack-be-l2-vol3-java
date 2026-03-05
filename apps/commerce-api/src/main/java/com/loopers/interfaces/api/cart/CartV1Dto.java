package com.loopers.interfaces.api.cart;

import com.loopers.application.cart.CartInfo;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class CartV1Dto {

    public record AddRequest(
        @NotNull Long productId,
        @Min(1) int quantity
    ) {}

    public record UpdateQuantityRequest(
        @Min(1) int quantity
    ) {}

    public record CartItemResponse(
        Long id,
        Long productId,
        String productName,
        int productPrice,
        String sellingStatus,
        int quantity,
        int subtotal
    ) {
        public static CartItemResponse from(CartInfo info) {
            return new CartItemResponse(
                info.id(),
                info.productId(),
                info.productName(),
                info.productPrice(),
                info.sellingStatus().name(),
                info.quantity(),
                info.productPrice() * info.quantity()
            );
        }
    }

    public record CartResponse(List<CartItemResponse> items, int totalPrice) {
        public static CartResponse from(List<CartInfo> infos) {
            List<CartItemResponse> items = infos.stream()
                .map(CartItemResponse::from)
                .toList();
            int totalPrice = infos.stream()
                .mapToInt(info -> info.productPrice() * info.quantity())
                .sum();
            return new CartResponse(items, totalPrice);
        }
    }
}
