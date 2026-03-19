package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderItemInfo;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class OrderV1Dto {

    public record CreateRequest(
        @NotNull Long productId,
        @Min(1) int quantity,
        Long couponId,
        @NotBlank String cardType,
        @NotBlank String cardNo
    ) {}

    public record OrderItemResponse(Long productId, String productName, String brandName, int quantity, int unitPrice, int subtotal) {
        public static OrderItemResponse from(OrderItemInfo info) {
            return new OrderItemResponse(
                info.productId(),
                info.productName(),
                info.brandName(),
                info.quantity(),
                info.unitPrice(),
                info.quantity() * info.unitPrice()
            );
        }
    }

    public record OrderResponse(
        Long id,
        String status,
        int originalTotalPrice,
        int discountAmount,
        int finalTotalPrice,
        Long userCouponId,
        List<OrderItemResponse> items
    ) {
        public static OrderResponse from(OrderInfo info) {
            List<OrderItemResponse> items = info.items().stream()
                .map(OrderItemResponse::from)
                .toList();
            return new OrderResponse(
                info.id(),
                info.status(),
                info.originalTotalPrice(),
                info.discountAmount(),
                info.finalTotalPrice(),
                info.userCouponId(),
                items
            );
        }
    }
}
