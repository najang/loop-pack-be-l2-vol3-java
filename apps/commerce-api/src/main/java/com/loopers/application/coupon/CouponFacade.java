package com.loopers.application.coupon;

import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.CouponTemplate;
import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.coupon.UserCoupon;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@RequiredArgsConstructor
@Component
public class CouponFacade {

    private final CouponService couponService;

    public UserCouponInfo issue(Long userId, Long couponTemplateId) {
        UserCoupon userCoupon = couponService.issue(userId, couponTemplateId);
        CouponTemplate template = couponService.findTemplateById(userCoupon.getCouponTemplateId());
        return UserCouponInfo.from(userCoupon, template);
    }

    @Transactional(readOnly = true)
    public List<UserCouponInfo> findMyUserCoupons(Long userId) {
        List<UserCoupon> userCoupons = couponService.findByUserId(userId);
        return userCoupons.stream()
            .map(uc -> {
                CouponTemplate template = couponService.findTemplateById(uc.getCouponTemplateId());
                return UserCouponInfo.from(uc, template);
            })
            .toList();
    }

    public CouponInfo createTemplate(String name, CouponType type, int value, Integer minOrderAmount, ZonedDateTime expiredAt) {
        CouponTemplate template = new CouponTemplate(name, type, value, minOrderAmount, expiredAt);
        return CouponInfo.from(couponService.saveTemplate(template));
    }

    @Transactional(readOnly = true)
    public Page<CouponInfo> findTemplates(Pageable pageable) {
        return couponService.findAllTemplates(pageable).map(CouponInfo::from);
    }

    @Transactional(readOnly = true)
    public CouponInfo findTemplateById(Long id) {
        return CouponInfo.from(couponService.findTemplateById(id));
    }

    public CouponInfo updateTemplate(Long id, String name, CouponType type, int value, Integer minOrderAmount, ZonedDateTime expiredAt) {
        CouponTemplate template = couponService.findTemplateById(id);
        template.update(name, type, value, minOrderAmount, expiredAt);
        return CouponInfo.from(couponService.saveTemplate(template));
    }

    public void deleteTemplate(Long id) {
        CouponTemplate template = couponService.findTemplateById(id);
        template.delete();
        couponService.saveTemplate(template);
    }

    @Transactional(readOnly = true)
    public Page<UserCouponInfo> findIssuedCoupons(Long couponTemplateId, Pageable pageable) {
        couponService.findTemplateById(couponTemplateId);
        return couponService.findByCouponTemplateId(couponTemplateId, pageable)
            .map(uc -> {
                CouponTemplate template = couponService.findTemplateById(uc.getCouponTemplateId());
                return UserCouponInfo.from(uc, template);
            });
    }
}
