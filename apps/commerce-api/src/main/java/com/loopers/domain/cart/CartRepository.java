package com.loopers.domain.cart;

import java.util.List;
import java.util.Optional;

public interface CartRepository {
    Optional<Cart> findByUserIdAndProductId(Long userId, Long productId);
    List<Cart> findByUserId(Long userId);
    Cart save(Cart cart);
}
