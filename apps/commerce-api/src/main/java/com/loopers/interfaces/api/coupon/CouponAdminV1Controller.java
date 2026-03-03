package com.loopers.interfaces.api.coupon;

import com.loopers.application.coupon.CouponFacade;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/api-admin/v1/coupons")
public class CouponAdminV1Controller implements CouponAdminV1ApiSpec {

    private final CouponFacade couponFacade;

    @GetMapping
    @Override
    public ApiResponse<CouponAdminV1Dto.CouponPageResponse> getCoupons(
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.success(CouponAdminV1Dto.CouponPageResponse.from(couponFacade.findTemplates(pageable)));
    }

    @GetMapping("/{couponId}")
    @Override
    public ApiResponse<CouponAdminV1Dto.CouponResponse> getCoupon(@PathVariable Long couponId) {
        return ApiResponse.success(CouponAdminV1Dto.CouponResponse.from(couponFacade.findTemplateById(couponId)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Override
    public ApiResponse<CouponAdminV1Dto.CouponResponse> createCoupon(
        @Valid @RequestBody CouponAdminV1Dto.CreateRequest request
    ) {
        return ApiResponse.success(CouponAdminV1Dto.CouponResponse.from(
            couponFacade.createTemplate(request.name(), request.type(), request.value(), request.minOrderAmount(), request.expiredAt())
        ));
    }

    @PutMapping("/{couponId}")
    @Override
    public ApiResponse<CouponAdminV1Dto.CouponResponse> updateCoupon(
        @PathVariable Long couponId,
        @Valid @RequestBody CouponAdminV1Dto.UpdateRequest request
    ) {
        return ApiResponse.success(CouponAdminV1Dto.CouponResponse.from(
            couponFacade.updateTemplate(couponId, request.name(), request.type(), request.value(), request.minOrderAmount(), request.expiredAt())
        ));
    }

    @DeleteMapping("/{couponId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Override
    public void deleteCoupon(@PathVariable Long couponId) {
        couponFacade.deleteTemplate(couponId);
    }

    @GetMapping("/{couponId}/issues")
    @Override
    public ApiResponse<CouponAdminV1Dto.UserCouponPageResponse> getIssuedCoupons(
        @PathVariable Long couponId,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.success(CouponAdminV1Dto.UserCouponPageResponse.from(
            couponFacade.findIssuedCoupons(couponId, pageable)
        ));
    }
}
