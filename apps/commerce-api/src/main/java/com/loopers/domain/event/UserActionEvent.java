package com.loopers.domain.event;

public record UserActionEvent(EventType eventType, Long userId, Long targetId, String metadata) {

    public enum EventType {
        PRODUCT_VIEWED,
        LIKED,
        UNLIKED,
        ORDER_CREATED,
        COUPON_ISSUED
    }
}
