package com.loopers.application.order;

import com.loopers.domain.order.OrderItem;

public record OrderItemInfo(Long productId, String productName, String brandName, int quantity, int unitPrice) {
    public static OrderItemInfo from(OrderItem item) {
        return new OrderItemInfo(item.getProductId(), item.getProductName(), item.getBrandName(), item.getQuantity(), item.getUnitPrice());
    }
}
