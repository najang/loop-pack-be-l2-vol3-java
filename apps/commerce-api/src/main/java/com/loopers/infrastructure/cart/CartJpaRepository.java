package com.loopers.infrastructure.cart;

import com.loopers.domain.cart.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartJpaRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserIdAndProductIdAndDeletedAtIsNull(Long userId, Long productId);
    List<Cart> findByUserIdAndDeletedAtIsNull(Long userId);
}
