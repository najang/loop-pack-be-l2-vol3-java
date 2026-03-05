package com.loopers.interfaces.api.brand;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;

@Tag(name = "Brand Admin V1 API", description = "브랜드 관련 어드민 API 입니다.")
public interface BrandAdminV1ApiSpec {

    @Operation(
        summary = "브랜드 목록 조회",
        description = "브랜드 목록을 페이징하여 조회합니다."
    )
    ApiResponse<BrandAdminV1Dto.BrandPageResponse> getBrands(Pageable pageable);

    @Operation(
        summary = "브랜드 단건 조회",
        description = "브랜드 ID로 브랜드 정보를 조회합니다."
    )
    ApiResponse<BrandAdminV1Dto.BrandResponse> getBrand(Long brandId);

    @Operation(
        summary = "브랜드 등록",
        description = "새로운 브랜드를 등록합니다."
    )
    ApiResponse<BrandAdminV1Dto.BrandResponse> createBrand(BrandAdminV1Dto.CreateRequest request);

    @Operation(
        summary = "브랜드 수정",
        description = "브랜드 정보를 수정합니다."
    )
    ApiResponse<BrandAdminV1Dto.BrandResponse> updateBrand(Long brandId, BrandAdminV1Dto.UpdateRequest request);

    @Operation(
        summary = "브랜드 삭제",
        description = "브랜드와 연관된 상품을 함께 삭제합니다."
    )
    void deleteBrand(Long brandId);
}
