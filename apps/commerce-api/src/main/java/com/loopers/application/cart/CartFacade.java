package com.loopers.application.cart;

import com.loopers.domain.cart.Cart;
import com.loopers.domain.cart.CartService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class CartFacade {

    private final CartService cartService;
    private final ProductService productService;

    public CartInfo add(Long userId, Long productId, int quantity) {
        Product product = productService.findById(productId);
        Cart cart = cartService.add(userId, productId, quantity);
        return CartInfo.from(cart, product);
    }

    @Transactional(readOnly = true)
    public List<CartInfo> findByUserId(Long userId) {
        List<Cart> carts = cartService.findByUserId(userId);
        if (carts.isEmpty()) {
            return List.of();
        }
        List<Long> productIds = carts.stream().map(Cart::getProductId).toList();
        Map<Long, Product> productMap = productService.findAllByIds(productIds).stream()
            .collect(Collectors.toMap(Product::getId, Function.identity()));
        return carts.stream()
            .filter(cart -> productMap.containsKey(cart.getProductId()))
            .map(cart -> CartInfo.from(cart, productMap.get(cart.getProductId())))
            .toList();
    }

    public CartInfo updateQuantity(Long userId, Long productId, int quantity) {
        Product product = productService.findById(productId);
        Cart cart = cartService.updateQuantity(userId, productId, quantity);
        return CartInfo.from(cart, product);
    }

    public void remove(Long userId, Long productId) {
        cartService.remove(userId, productId);
    }
}
