package com.loopers.interfaces.consumer;

import com.loopers.domain.coupon.CouponIssueResult;
import com.loopers.domain.coupon.CouponIssueStatus;
import com.loopers.domain.coupon.UserCoupon;
import com.loopers.infrastructure.coupon.CouponIssueResultStreamerJpaRepository;
import com.loopers.infrastructure.coupon.CouponTemplateStreamerJpaRepository;
import com.loopers.infrastructure.coupon.UserCouponStreamerJpaRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponIssueConsumerTest {

    @Mock
    private CouponTemplateStreamerJpaRepository couponTemplateRepository;

    @Mock
    private UserCouponStreamerJpaRepository userCouponRepository;

    @Mock
    private CouponIssueResultStreamerJpaRepository couponIssueResultRepository;

    @InjectMocks
    private CouponIssueConsumer couponIssueConsumer;

    @DisplayName("handleCouponIssueEvents() 시,")
    @Nested
    class HandleCouponIssueEvents {

        @DisplayName("수량이 남아있고 중복이 아니면, SUCCESS로 마킹한다.")
        @Test
        void marksSuccess_whenQuantityAvailableAndNoDuplicate() {
            // arrange
            CouponIssueConsumer.CouponIssueMessage message = new CouponIssueConsumer.CouponIssueMessage("req-1", 1L, 100L, 100);
            ConsumerRecord<String, CouponIssueConsumer.CouponIssueMessage> record = new ConsumerRecord<>("coupon-issue-requests", 0, 0L, "100", message);
            Acknowledgment ack = mock(Acknowledgment.class);

            CouponIssueResult result = mock(CouponIssueResult.class);
            when(couponIssueResultRepository.findByRequestId("req-1")).thenReturn(Optional.of(result));
            when(couponTemplateRepository.incrementIssuedCount(100L, 100)).thenReturn(1);

            // act
            couponIssueConsumer.handleCouponIssueEvents(List.of(record), ack);

            // assert
            verify(userCouponRepository, times(1)).save(any(UserCoupon.class));
            verify(result, times(1)).markSuccess();
            verify(couponIssueResultRepository, times(1)).save(result);
            verify(ack, times(1)).acknowledge();
        }

        @DisplayName("수량이 소진되었으면, SOLD_OUT으로 마킹한다.")
        @Test
        void marksSoldOut_whenQuantityExhausted() {
            // arrange
            CouponIssueConsumer.CouponIssueMessage message = new CouponIssueConsumer.CouponIssueMessage("req-2", 1L, 100L, 100);
            ConsumerRecord<String, CouponIssueConsumer.CouponIssueMessage> record = new ConsumerRecord<>("coupon-issue-requests", 0, 0L, "100", message);
            Acknowledgment ack = mock(Acknowledgment.class);

            CouponIssueResult result = mock(CouponIssueResult.class);
            when(couponIssueResultRepository.findByRequestId("req-2")).thenReturn(Optional.of(result));
            when(couponTemplateRepository.incrementIssuedCount(100L, 100)).thenReturn(0);

            // act
            couponIssueConsumer.handleCouponIssueEvents(List.of(record), ack);

            // assert
            verify(userCouponRepository, never()).save(any());
            verify(result, times(1)).markSoldOut();
            verify(couponIssueResultRepository, times(1)).save(result);
            verify(ack, times(1)).acknowledge();
        }

        @DisplayName("중복 발급이면, DUPLICATE로 마킹하고 issued_count를 롤백한다.")
        @Test
        void marksDuplicate_andRollsBackCount_whenDuplicateIssue() {
            // arrange
            CouponIssueConsumer.CouponIssueMessage message = new CouponIssueConsumer.CouponIssueMessage("req-3", 1L, 100L, 100);
            ConsumerRecord<String, CouponIssueConsumer.CouponIssueMessage> record = new ConsumerRecord<>("coupon-issue-requests", 0, 0L, "100", message);
            Acknowledgment ack = mock(Acknowledgment.class);

            CouponIssueResult result = mock(CouponIssueResult.class);
            when(couponIssueResultRepository.findByRequestId("req-3")).thenReturn(Optional.of(result));
            when(couponTemplateRepository.incrementIssuedCount(100L, 100)).thenReturn(1);
            when(userCouponRepository.save(any(UserCoupon.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

            // act
            couponIssueConsumer.handleCouponIssueEvents(List.of(record), ack);

            // assert
            verify(result, times(1)).markDuplicate();
            verify(couponTemplateRepository, times(1)).decrementIssuedCount(100L);
            verify(couponIssueResultRepository, times(1)).save(result);
            verify(ack, times(1)).acknowledge();
        }

        @DisplayName("null 메시지는 무시하고 ack한다.")
        @Test
        void skipsNull_andAcknowledges() {
            // arrange
            ConsumerRecord<String, CouponIssueConsumer.CouponIssueMessage> record = new ConsumerRecord<>("coupon-issue-requests", 0, 0L, "100", null);
            Acknowledgment ack = mock(Acknowledgment.class);

            // act
            couponIssueConsumer.handleCouponIssueEvents(List.of(record), ack);

            // assert
            verify(couponIssueResultRepository, never()).findByRequestId(any());
            verify(ack, times(1)).acknowledge();
        }
    }
}
