package com.loopers.interfaces.api.coupon;

import com.loopers.application.coupon.CouponFacade;
import com.loopers.application.coupon.CouponInfo;
import com.loopers.domain.user.UserModel;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.auth.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class CouponV1Controller implements CouponV1ApiSpec {

    private final CouponFacade couponFacade;

    @PostMapping("/coupons/{couponId}/issue")
    @Override
    public ResponseEntity<ApiResponse<?>> issueCoupon(
        @LoginUser UserModel user,
        @PathVariable Long couponId
    ) {
        CouponInfo templateInfo = couponFacade.findTemplateById(couponId);
        if (templateInfo.maxQuantity() != null) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                ApiResponse.success(CouponV1Dto.CouponIssueRequestResponse.from(
                    couponFacade.issueAsync(user.getId(), couponId)
                ))
            );
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.success(CouponV1Dto.UserCouponResponse.from(
                couponFacade.issue(user.getId(), couponId)
            ))
        );
    }

    @GetMapping("/coupons/issue/{requestId}")
    public ApiResponse<CouponV1Dto.CouponIssueResultResponse> getIssueResult(
        @LoginUser UserModel user,
        @PathVariable String requestId
    ) {
        return ApiResponse.success(CouponV1Dto.CouponIssueResultResponse.from(
            couponFacade.findIssueResult(requestId)
        ));
    }

    @GetMapping("/users/me/coupons")
    @Override
    public ApiResponse<List<CouponV1Dto.UserCouponResponse>> getMyCoupons(
        @LoginUser UserModel user
    ) {
        List<CouponV1Dto.UserCouponResponse> coupons = couponFacade.findMyUserCoupons(user.getId()).stream()
            .map(CouponV1Dto.UserCouponResponse::from)
            .toList();
        return ApiResponse.success(coupons);
    }
}
