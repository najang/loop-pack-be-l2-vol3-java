package com.loopers.domain.coupon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class CouponIssueResultTest {

    @DisplayName("CouponIssueResult 생성 시,")
    @Nested
    class Create {

        @DisplayName("PENDING 상태와 UUID requestId가 생성된다.")
        @Test
        void createsWithPendingStatusAndRequestId() {
            // act
            CouponIssueResult result = new CouponIssueResult(1L, 100L);

            // assert
            assertAll(
                () -> assertThat(result.getRequestId()).isNotNull(),
                () -> assertThat(result.getRequestId()).hasSize(36),
                () -> assertThat(result.getUserId()).isEqualTo(1L),
                () -> assertThat(result.getCouponTemplateId()).isEqualTo(100L),
                () -> assertThat(result.getStatus()).isEqualTo(CouponIssueStatus.PENDING)
            );
        }
    }

    @DisplayName("상태 전이 시,")
    @Nested
    class StatusTransition {

        @DisplayName("markSuccess()를 호출하면 SUCCESS 상태가 된다.")
        @Test
        void transitionsToSuccess() {
            // arrange
            CouponIssueResult result = new CouponIssueResult(1L, 100L);

            // act
            result.markSuccess();

            // assert
            assertThat(result.getStatus()).isEqualTo(CouponIssueStatus.SUCCESS);
        }

        @DisplayName("markSoldOut()을 호출하면 SOLD_OUT 상태가 된다.")
        @Test
        void transitionsToSoldOut() {
            // arrange
            CouponIssueResult result = new CouponIssueResult(1L, 100L);

            // act
            result.markSoldOut();

            // assert
            assertThat(result.getStatus()).isEqualTo(CouponIssueStatus.SOLD_OUT);
        }

        @DisplayName("markDuplicate()를 호출하면 DUPLICATE 상태가 된다.")
        @Test
        void transitionsToDuplicate() {
            // arrange
            CouponIssueResult result = new CouponIssueResult(1L, 100L);

            // act
            result.markDuplicate();

            // assert
            assertThat(result.getStatus()).isEqualTo(CouponIssueStatus.DUPLICATE);
        }
    }
}
