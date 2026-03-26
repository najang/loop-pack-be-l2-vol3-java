package com.loopers.application.coupon;

import com.loopers.domain.coupon.CouponIssueResult;
import com.loopers.domain.coupon.CouponIssueResultRepository;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.CouponTemplate;
import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.event.UserActionEvent;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@RequiredArgsConstructor
@Component
public class CouponFacade {

    private static final String COUPON_ISSUE_TOPIC = "coupon-issue-requests";

    private final CouponService couponService;
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final CouponIssueResultRepository couponIssueResultRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UserCouponInfo issue(Long userId, Long couponTemplateId) {
        UserCoupon userCoupon = couponService.issue(userId, couponTemplateId);
        CouponTemplate template = couponService.findTemplateById(userCoupon.getCouponTemplateId());
        UserCouponInfo info = UserCouponInfo.from(userCoupon, template);
        eventPublisher.publishEvent(new UserActionEvent(
            UserActionEvent.EventType.COUPON_ISSUED, userId, info.id(), null
        ));
        return info;
    }

    @Transactional
    public CouponIssueRequestInfo issueAsync(Long userId, Long couponTemplateId) {
        CouponTemplate template = couponService.findTemplateById(couponTemplateId);
        if (!template.canIssue()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "발급 가능한 쿠폰이 아닙니다.");
        }

        CouponIssueResult result = couponIssueResultRepository.save(new CouponIssueResult(userId, couponTemplateId));
        CouponIssueMessage message = new CouponIssueMessage(result.getRequestId(), userId, couponTemplateId, template.getMaxQuantity());
        kafkaTemplate.send(COUPON_ISSUE_TOPIC, couponTemplateId.toString(), message);
        return new CouponIssueRequestInfo(result.getRequestId());
    }

    @Transactional(readOnly = true)
    public CouponIssueResultInfo findIssueResult(String requestId) {
        CouponIssueResult result = couponIssueResultRepository.findByRequestId(requestId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "발급 요청을 찾을 수 없습니다."));
        return new CouponIssueResultInfo(result.getRequestId(), result.getCouponTemplateId(), result.getStatus().name());
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

    public CouponInfo createTemplate(String name, CouponType type, int value, Integer minOrderAmount, ZonedDateTime expiredAt, Integer maxQuantity) {
        CouponTemplate template = new CouponTemplate(name, type, value, minOrderAmount, expiredAt, maxQuantity);
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
