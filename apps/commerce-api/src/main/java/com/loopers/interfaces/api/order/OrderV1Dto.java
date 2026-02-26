package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderItemInfo;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class OrderV1Dto {

    public record CreateRequest(
        @NotNull Long productId,
        @Min(1) int quantity
    ) {}

    public record OrderItemResponse(Long productId, int quantity, int unitPrice, int subtotal) {
        public static OrderItemResponse from(OrderItemInfo info) {
            return new OrderItemResponse(
                info.productId(),
                info.quantity(),
                info.unitPrice(),
                info.quantity() * info.unitPrice()
            );
        }
    }

    public record OrderResponse(Long id, String status, int totalPrice, List<OrderItemResponse> items) {
        public static OrderResponse from(OrderInfo info) {
            List<OrderItemResponse> items = info.items().stream()
                .map(OrderItemResponse::from)
                .toList();
            return new OrderResponse(info.id(), info.status(), info.totalPrice(), items);
        }
    }
}
