package com.loopers.domain.cart;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Component
public class CartService {

    private final CartRepository cartRepository;

    @Transactional
    public Cart add(Long userId, Long productId, int quantity) {
        return cartRepository.findByUserIdAndProductId(userId, productId)
            .map(cart -> {
                cart.addQuantity(quantity);
                return cartRepository.save(cart);
            })
            .orElseGet(() -> cartRepository.save(new Cart(userId, productId, quantity)));
    }

    @Transactional(readOnly = true)
    public List<Cart> findByUserId(Long userId) {
        return cartRepository.findByUserId(userId);
    }

    @Transactional
    public void remove(Long userId, Long productId) {
        Cart cart = cartRepository.findByUserIdAndProductId(userId, productId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "장바구니 항목을 찾을 수 없습니다."));
        cart.delete();
        cartRepository.save(cart);
    }
}
