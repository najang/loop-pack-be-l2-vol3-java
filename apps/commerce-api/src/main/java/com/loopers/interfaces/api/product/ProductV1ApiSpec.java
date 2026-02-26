package com.loopers.interfaces.api.product;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Product V1 API", description = "상품 관련 사용자 API 입니다.")
public interface ProductV1ApiSpec {

    @Operation(
        summary = "상품 목록 조회",
        description = "상품 목록을 페이징하여 조회합니다."
    )
    ApiResponse<ProductV1Dto.ProductPageResponse> getProducts(Long brandId, String sort, int page, int size);

    @Operation(
        summary = "상품 단건 조회",
        description = "상품 ID로 상품 정보를 조회합니다."
    )
    ApiResponse<ProductV1Dto.ProductResponse> getProduct(Long productId);
}
