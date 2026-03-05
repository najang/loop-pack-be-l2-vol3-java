package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@RequiredArgsConstructor
@Component
public class CouponService {

    private final CouponTemplateRepository couponTemplateRepository;
    private final UserCouponRepository userCouponRepository;

    @Transactional(readOnly = true)
    public CouponTemplate findTemplateById(Long id) {
        return couponTemplateRepository.findById(id)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰 템플릿을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public Page<CouponTemplate> findAllTemplates(Pageable pageable) {
        return couponTemplateRepository.findAll(pageable);
    }

    @Transactional
    public CouponTemplate saveTemplate(CouponTemplate couponTemplate) {
        return couponTemplateRepository.save(couponTemplate);
    }

    @Transactional
    public UserCoupon issue(Long userId, Long couponTemplateId) {
        CouponTemplate template = couponTemplateRepository.findById(couponTemplateId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰 템플릿을 찾을 수 없습니다."));

        if (!template.canIssue()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "발급 가능한 쿠폰이 아닙니다.");
        }

        if (userCouponRepository.existsByUserIdAndCouponTemplateId(userId, couponTemplateId)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 발급된 쿠폰입니다.");
        }

        UserCoupon userCoupon = new UserCoupon(userId, couponTemplateId);
        return userCouponRepository.save(userCoupon);
    }

    @Transactional(readOnly = true)
    public List<UserCoupon> findByUserId(Long userId) {
        return userCouponRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Page<UserCoupon> findByCouponTemplateId(Long couponTemplateId, Pageable pageable) {
        return userCouponRepository.findByCouponTemplateId(couponTemplateId, pageable);
    }

    @Transactional(readOnly = true)
    public UserCoupon findUserCouponById(Long id) {
        return userCouponRepository.findById(id)
            .orElseThrow(() -> new CoreException(ErrorType.BAD_REQUEST, "쿠폰을 찾을 수 없습니다."));
    }

    @Transactional
    public int validateAndUse(Long userId, Long userCouponId, int orderAmount) {
        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
            .orElseThrow(() -> new CoreException(ErrorType.BAD_REQUEST, "쿠폰을 찾을 수 없습니다."));

        if (!userCoupon.getUserId().equals(userId)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "본인의 쿠폰이 아닙니다.");
        }

        CouponTemplate template = couponTemplateRepository.findById(userCoupon.getCouponTemplateId())
            .orElseThrow(() -> new CoreException(ErrorType.BAD_REQUEST, "쿠폰 템플릿을 찾을 수 없습니다."));

        userCoupon.canUse(orderAmount, template);
        int discountAmount = template.calculateDiscount(orderAmount);

        int affected = userCouponRepository.useIfAvailable(userCouponId, ZonedDateTime.now());
        if (affected == 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 사용된 쿠폰입니다.");
        }

        return discountAmount;
    }
}
