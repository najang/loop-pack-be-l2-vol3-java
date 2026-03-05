package com.loopers.interfaces.api.cart;

import com.loopers.application.cart.CartFacade;
import com.loopers.domain.user.UserModel;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.auth.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class CartV1Controller implements CartV1ApiSpec {

    private final CartFacade cartFacade;

    @PostMapping("/cart/items")
    @ResponseStatus(HttpStatus.CREATED)
    @Override
    public ApiResponse<CartV1Dto.CartItemResponse> addItem(
        @LoginUser UserModel user,
        @Valid @RequestBody CartV1Dto.AddRequest request
    ) {
        return ApiResponse.success(CartV1Dto.CartItemResponse.from(
            cartFacade.add(user.getId(), request.productId(), request.quantity())
        ));
    }

    @GetMapping("/cart")
    @Override
    public ApiResponse<CartV1Dto.CartResponse> getCart(@LoginUser UserModel user) {
        return ApiResponse.success(CartV1Dto.CartResponse.from(cartFacade.findByUserId(user.getId())));
    }

    @PutMapping("/cart/items/{productId}")
    @Override
    public ApiResponse<CartV1Dto.CartItemResponse> updateQuantity(
        @LoginUser UserModel user,
        @PathVariable Long productId,
        @Valid @RequestBody CartV1Dto.UpdateQuantityRequest request
    ) {
        return ApiResponse.success(CartV1Dto.CartItemResponse.from(
            cartFacade.updateQuantity(user.getId(), productId, request.quantity())
        ));
    }

    @DeleteMapping("/cart/items/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Override
    public void removeItem(
        @LoginUser UserModel user,
        @PathVariable Long productId
    ) {
        cartFacade.remove(user.getId(), productId);
    }
}
