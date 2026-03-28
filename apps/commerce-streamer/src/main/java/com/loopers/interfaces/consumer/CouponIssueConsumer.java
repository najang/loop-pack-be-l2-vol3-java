package com.loopers.interfaces.consumer;

import com.loopers.confg.kafka.KafkaConfig;
import com.loopers.domain.coupon.CouponIssueResult;
import com.loopers.domain.coupon.UserCoupon;
import com.loopers.infrastructure.coupon.CouponIssueResultStreamerJpaRepository;
import com.loopers.infrastructure.coupon.CouponTemplateStreamerJpaRepository;
import com.loopers.infrastructure.coupon.UserCouponStreamerJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class CouponIssueConsumer {

    private final CouponTemplateStreamerJpaRepository couponTemplateRepository;
    private final UserCouponStreamerJpaRepository userCouponRepository;
    private final CouponIssueResultStreamerJpaRepository couponIssueResultRepository;

    @Transactional
    @KafkaListener(topics = "coupon-issue-requests", containerFactory = KafkaConfig.BATCH_LISTENER)
    public void handleCouponIssueEvents(
        List<ConsumerRecord<String, CouponIssueMessage>> messages,
        Acknowledgment acknowledgment
    ) {
        for (ConsumerRecord<String, CouponIssueMessage> record : messages) {
            CouponIssueMessage message = record.value();
            if (message == null) {
                continue;
            }
            try {
                processCouponIssue(message);
            } catch (Exception e) {
                log.error("CouponIssue 처리 실패: requestId={}, couponTemplateId={}", message.requestId(), message.couponTemplateId(), e);
            }
        }
        acknowledgment.acknowledge();
    }

    private void processCouponIssue(CouponIssueMessage message) {
        CouponIssueResult result = couponIssueResultRepository.findByRequestId(message.requestId())
            .orElse(null);
        if (result == null) {
            log.warn("CouponIssueResult를 찾을 수 없습니다: requestId={}", message.requestId());
            return;
        }

        int affected = couponTemplateRepository.incrementIssuedCount(message.couponTemplateId(), message.maxQuantity());
        if (affected == 0) {
            result.markSoldOut();
            couponIssueResultRepository.save(result);
            return;
        }

        try {
            userCouponRepository.save(new UserCoupon(message.userId(), message.couponTemplateId()));
            result.markSuccess();
        } catch (DataIntegrityViolationException e) {
            couponTemplateRepository.decrementIssuedCount(message.couponTemplateId());
            result.markDuplicate();
        }
        couponIssueResultRepository.save(result);
    }

    public record CouponIssueMessage(String requestId, Long userId, Long couponTemplateId, int maxQuantity) {
    }
}
