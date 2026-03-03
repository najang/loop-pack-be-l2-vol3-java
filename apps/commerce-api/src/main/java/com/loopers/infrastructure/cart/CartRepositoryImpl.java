package com.loopers.infrastructure.cart;

import com.loopers.domain.cart.Cart;
import com.loopers.domain.cart.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class CartRepositoryImpl implements CartRepository {

    private final CartJpaRepository cartJpaRepository;

    @Override
    public Optional<Cart> findByUserIdAndProductId(Long userId, Long productId) {
        return cartJpaRepository.findByUserIdAndProductIdAndDeletedAtIsNull(userId, productId);
    }

    @Override
    public List<Cart> findByUserId(Long userId) {
        return cartJpaRepository.findByUserIdAndDeletedAtIsNull(userId);
    }

    @Override
    public Cart save(Cart cart) {
        return cartJpaRepository.save(cart);
    }
}
