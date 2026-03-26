package com.loopers.domain.event;

import java.util.UUID;

public record LikeEvent(Type type, Long productId, Long userId, String eventId) {

    public enum Type {
        LIKED, UNLIKED
    }

    public static LikeEvent of(Type type, Long productId, Long userId) {
        return new LikeEvent(type, productId, userId, UUID.randomUUID().toString());
    }
}
