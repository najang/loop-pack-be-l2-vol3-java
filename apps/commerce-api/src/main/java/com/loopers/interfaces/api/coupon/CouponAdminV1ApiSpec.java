package com.loopers.interfaces.api.coupon;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;

@Tag(name = "Coupon Admin V1 API", description = "쿠폰 관련 어드민 API입니다.")
public interface CouponAdminV1ApiSpec {

    @Operation(summary = "쿠폰 템플릿 목록 조회", description = "쿠폰 템플릿 목록을 페이징하여 조회합니다.")
    ApiResponse<CouponAdminV1Dto.CouponPageResponse> getCoupons(Pageable pageable);

    @Operation(summary = "쿠폰 템플릿 단건 조회", description = "쿠폰 템플릿 ID로 상세 정보를 조회합니다.")
    ApiResponse<CouponAdminV1Dto.CouponResponse> getCoupon(Long couponId);

    @Operation(summary = "쿠폰 템플릿 등록", description = "새로운 쿠폰 템플릿을 등록합니다.")
    ApiResponse<CouponAdminV1Dto.CouponResponse> createCoupon(CouponAdminV1Dto.CreateRequest request);

    @Operation(summary = "쿠폰 템플릿 수정", description = "쿠폰 템플릿 정보를 수정합니다.")
    ApiResponse<CouponAdminV1Dto.CouponResponse> updateCoupon(Long couponId, CouponAdminV1Dto.UpdateRequest request);

    @Operation(summary = "쿠폰 템플릿 삭제", description = "쿠폰 템플릿을 soft delete 처리합니다.")
    void deleteCoupon(Long couponId);

    @Operation(summary = "특정 쿠폰 발급 내역 조회", description = "특정 쿠폰 템플릿의 발급 내역을 페이징하여 조회합니다.")
    ApiResponse<CouponAdminV1Dto.UserCouponPageResponse> getIssuedCoupons(Long couponId, Pageable pageable);
}
