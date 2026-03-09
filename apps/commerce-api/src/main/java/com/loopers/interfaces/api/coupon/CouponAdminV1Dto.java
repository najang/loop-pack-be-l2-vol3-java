package com.loopers.interfaces.api.coupon;

import com.loopers.application.coupon.CouponInfo;
import com.loopers.application.coupon.UserCouponInfo;
import com.loopers.domain.coupon.CouponType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;

import java.time.ZonedDateTime;
import java.util.List;

public class CouponAdminV1Dto {

    public record CreateRequest(
        @NotBlank(message = "쿠폰명은 비어있을 수 없습니다.")
        String name,
        @NotNull(message = "쿠폰 타입은 필수입니다.")
        CouponType type,
        @NotNull(message = "쿠폰 할인 값은 필수입니다.")
        @Positive(message = "쿠폰 할인 값은 0보다 커야 합니다.")
        Integer value,
        Integer minOrderAmount,
        @NotNull(message = "쿠폰 유효 기간은 필수입니다.")
        ZonedDateTime expiredAt
    ) {}

    public record UpdateRequest(
        @NotBlank(message = "쿠폰명은 비어있을 수 없습니다.")
        String name,
        @NotNull(message = "쿠폰 타입은 필수입니다.")
        CouponType type,
        @NotNull(message = "쿠폰 할인 값은 필수입니다.")
        @Positive(message = "쿠폰 할인 값은 0보다 커야 합니다.")
        Integer value,
        Integer minOrderAmount,
        @NotNull(message = "쿠폰 유효 기간은 필수입니다.")
        ZonedDateTime expiredAt
    ) {}

    public record CouponResponse(
        Long id,
        String name,
        String type,
        int value,
        Integer minOrderAmount,
        ZonedDateTime expiredAt,
        boolean isActive
    ) {
        public static CouponResponse from(CouponInfo info) {
            return new CouponResponse(
                info.id(),
                info.name(),
                info.type(),
                info.value(),
                info.minOrderAmount(),
                info.expiredAt(),
                info.isActive()
            );
        }
    }

    public record CouponPageResponse(List<CouponResponse> content, int page, int size, long totalElements) {
        public static CouponPageResponse from(Page<CouponInfo> page) {
            List<CouponResponse> content = page.getContent().stream()
                .map(CouponResponse::from)
                .toList();
            return new CouponPageResponse(content, page.getNumber(), page.getSize(), page.getTotalElements());
        }
    }

    public record UserCouponResponse(
        Long id,
        Long userId,
        Long couponTemplateId,
        String status,
        ZonedDateTime usedAt
    ) {
        public static UserCouponResponse from(UserCouponInfo info) {
            return new UserCouponResponse(
                info.id(),
                info.userId(),
                info.couponTemplateId(),
                info.status(),
                info.usedAt()
            );
        }
    }

    public record UserCouponPageResponse(List<UserCouponResponse> content, int page, int size, long totalElements) {
        public static UserCouponPageResponse from(Page<UserCouponInfo> page) {
            List<UserCouponResponse> content = page.getContent().stream()
                .map(UserCouponResponse::from)
                .toList();
            return new UserCouponPageResponse(content, page.getNumber(), page.getSize(), page.getTotalElements());
        }
    }
}
