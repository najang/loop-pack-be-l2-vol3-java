package com.loopers.interfaces.api.coupon;

import com.loopers.domain.user.UserModel;
import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Coupon V1 API", description = "쿠폰 관련 사용자 API입니다.")
public interface CouponV1ApiSpec {

    @Operation(
        summary = "쿠폰 발급",
        description = "쿠폰 템플릿 ID로 쿠폰을 발급받습니다."
    )
    ApiResponse<CouponV1Dto.UserCouponResponse> issueCoupon(UserModel user, Long couponId);

    @Operation(
        summary = "내 쿠폰 목록 조회",
        description = "로그인한 사용자의 쿠폰 목록을 조회합니다."
    )
    ApiResponse<List<CouponV1Dto.UserCouponResponse>> getMyCoupons(UserModel user);
}
