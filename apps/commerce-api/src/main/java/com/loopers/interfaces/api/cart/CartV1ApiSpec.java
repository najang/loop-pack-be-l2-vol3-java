package com.loopers.interfaces.api.cart;

import com.loopers.domain.user.UserModel;
import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Cart V1 API", description = "장바구니 관련 사용자 API 입니다.")
public interface CartV1ApiSpec {

    @Operation(
        summary = "장바구니 담기",
        description = "상품을 장바구니에 담습니다. 이미 담긴 상품이면 수량을 추가합니다."
    )
    ApiResponse<CartV1Dto.CartItemResponse> addItem(
        @Parameter(hidden = true) UserModel user,
        CartV1Dto.AddRequest request
    );

    @Operation(
        summary = "장바구니 조회",
        description = "로그인한 사용자의 장바구니를 조회합니다. 품절 상품 여부도 포함됩니다."
    )
    ApiResponse<CartV1Dto.CartResponse> getCart(
        @Parameter(hidden = true) UserModel user
    );

    @Operation(
        summary = "장바구니 수량 변경",
        description = "장바구니의 특정 상품 수량을 변경합니다."
    )
    ApiResponse<CartV1Dto.CartItemResponse> updateQuantity(
        @Parameter(hidden = true) UserModel user,
        Long productId,
        CartV1Dto.UpdateQuantityRequest request
    );

    @Operation(
        summary = "장바구니 상품 삭제",
        description = "장바구니에서 특정 상품을 삭제합니다."
    )
    void removeItem(
        @Parameter(hidden = true) UserModel user,
        Long productId
    );
}
