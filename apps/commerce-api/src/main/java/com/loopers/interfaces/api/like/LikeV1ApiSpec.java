package com.loopers.interfaces.api.like;

import com.loopers.domain.user.UserModel;
import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Like V1 API", description = "좋아요 관련 사용자 API 입니다.")
public interface LikeV1ApiSpec {

    @Operation(
        summary = "좋아요 추가",
        description = "상품에 좋아요를 추가합니다. 이미 좋아요한 경우 현재 상태를 반환합니다."
    )
    ApiResponse<LikeV1Dto.LikeResponse> like(
        Long productId,
        @Parameter(hidden = true) UserModel user
    );

    @Operation(
        summary = "좋아요 취소",
        description = "상품의 좋아요를 취소합니다. 좋아요가 없는 경우에도 성공 응답을 반환합니다."
    )
    void unlike(
        Long productId,
        @Parameter(hidden = true) UserModel user
    );

    @Operation(
        summary = "좋아요 상품 목록 조회",
        description = "로그인한 사용자가 좋아요한 상품 목록을 페이징하여 조회합니다."
    )
    ApiResponse<LikeV1Dto.LikedProductPageResponse> getLikedProducts(
        @Parameter(hidden = true) UserModel user,
        int page,
        int size
    );
}
