package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderItemInfo;
import com.loopers.domain.order.OrderStatus;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class OrderAdminV1Dto {

    public record UpdateStatusRequest(
        @NotNull OrderStatus status
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

    public record OrderResponse(Long id, Long userId, String status, int totalPrice, List<OrderItemResponse> items) {
        public static OrderResponse from(OrderInfo info) {
            List<OrderItemResponse> items = info.items().stream()
                .map(OrderItemResponse::from)
                .toList();
            return new OrderResponse(info.id(), info.userId(), info.status(), info.totalPrice(), items);
        }
    }
}
