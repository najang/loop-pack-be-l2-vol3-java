package com.loopers.application.coupon;

import com.loopers.domain.coupon.CouponIssueResult;
import com.loopers.domain.coupon.CouponIssueResultRepository;
import com.loopers.domain.coupon.CouponIssueStatus;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.CouponTemplate;
import com.loopers.domain.coupon.CouponType;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponFacadeTest {

    @Mock
    private CouponService couponService;

    @Mock
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Mock
    private CouponIssueResultRepository couponIssueResultRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CouponFacade couponFacade;

    @DisplayName("issueAsync() 호출 시,")
    @Nested
    class IssueAsync {

        @DisplayName("FCFS 템플릿이면, PENDING 상태 저장 + Kafka 발행 + requestId 반환한다.")
        @Test
        void savesAndPublishes_whenFcfsTemplate() {
            // arrange
            Long userId = 1L;
            Long couponTemplateId = 100L;
            CouponTemplate template = new CouponTemplate("선착순", CouponType.FIXED, 1000, null, ZonedDateTime.now().plusDays(7), 100);
            when(couponService.findTemplateById(couponTemplateId)).thenReturn(template);
            when(couponIssueResultRepository.save(any(CouponIssueResult.class))).thenAnswer(inv -> inv.getArgument(0));

            // act
            CouponIssueRequestInfo info = couponFacade.issueAsync(userId, couponTemplateId);

            // assert
            assertThat(info.requestId()).isNotNull();
            verify(couponIssueResultRepository, times(1)).save(any(CouponIssueResult.class));
            verify(kafkaTemplate, times(1)).send(eq("coupon-issue-requests"), eq(couponTemplateId.toString()), any(CouponIssueMessage.class));
        }

        @DisplayName("canIssue()가 false이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenCannotIssue() {
            // arrange
            Long userId = 1L;
            Long couponTemplateId = 100L;
            CouponTemplate template = new CouponTemplate("만료", CouponType.FIXED, 1000, null, ZonedDateTime.now().minusDays(1), 100);
            when(couponService.findTemplateById(couponTemplateId)).thenReturn(template);

            // act
            CoreException ex = assertThrows(CoreException.class, () -> couponFacade.issueAsync(userId, couponTemplateId));

            // assert
            assertThat(ex.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("findIssueResult() 호출 시,")
    @Nested
    class FindIssueResult {

        @DisplayName("requestId가 존재하면, 결과를 반환한다.")
        @Test
        void returnsResult_whenRequestIdExists() {
            // arrange
            CouponIssueResult result = new CouponIssueResult(1L, 100L);
            when(couponIssueResultRepository.findByRequestId(result.getRequestId())).thenReturn(Optional.of(result));

            // act
            CouponIssueResultInfo info = couponFacade.findIssueResult(result.getRequestId());

            // assert
            assertThat(info.requestId()).isEqualTo(result.getRequestId());
            assertThat(info.status()).isEqualTo(CouponIssueStatus.PENDING.name());
        }

        @DisplayName("requestId가 존재하지 않으면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFound_whenRequestIdDoesNotExist() {
            // arrange
            when(couponIssueResultRepository.findByRequestId("nonexistent")).thenReturn(Optional.empty());

            // act
            CoreException ex = assertThrows(CoreException.class, () -> couponFacade.findIssueResult("nonexistent"));

            // assert
            assertThat(ex.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }
}
