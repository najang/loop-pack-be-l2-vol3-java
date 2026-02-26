package com.loopers.application.order;

import com.loopers.domain.order.Order;

import java.util.List;

public record OrderInfo(Long id, Long userId, String status, int totalPrice, List<OrderItemInfo> items) {
    public static OrderInfo from(Order order) {
        List<OrderItemInfo> items = order.getItems().stream()
            .map(OrderItemInfo::from)
            .toList();
        return new OrderInfo(order.getId(), order.getUserId(), order.getStatus().name(), order.getTotalPrice(), items);
    }
}
