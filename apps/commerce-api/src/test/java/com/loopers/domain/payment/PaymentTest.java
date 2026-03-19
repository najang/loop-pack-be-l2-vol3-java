package com.loopers.domain.payment;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class PaymentTest {

    @DisplayName("결제를 생성할 때,")
    @Nested
    class Create {

        @DisplayName("정상 생성 시 PENDING 상태로 초기화된다.")
        @Test
        void createsWithPendingStatus() {
            // arrange & act
            Payment payment = new Payment(1L, 10000, "SAMSUNG", "1234-5678-9012-3456");

            // assert
            assertAll(
                () -> assertThat(payment.getOrderId()).isEqualTo(1L),
                () -> assertThat(payment.getAmount()).isEqualTo(10000),
                () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING),
                () -> assertThat(payment.getCardType()).isEqualTo("SAMSUNG"),
                () -> assertThat(payment.getCardNo()).isEqualTo("1234-5678-9012-3456")
            );
        }

        @DisplayName("orderId가 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenOrderIdIsNull() {
            assertThatThrownBy(() -> new Payment(null, 10000, "SAMSUNG", "1234"))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("amount가 0 이하이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenAmountIsZeroOrNegative() {
            assertThatThrownBy(() -> new Payment(1L, 0, "SAMSUNG", "1234"))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }
    }

    @DisplayName("결제 완료 처리 시,")
    @Nested
    class Complete {

        @DisplayName("PENDING 상태에서 complete()을 호출하면, COMPLETED로 전이된다.")
        @Test
        void transitionsToCompleted_whenPending() {
            // arrange
            Payment payment = new Payment(1L, 10000, "SAMSUNG", "1234");

            // act
            payment.complete("TX-001");

            // assert
            assertAll(
                () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED),
                () -> assertThat(payment.getPgTransactionId()).isEqualTo("TX-001")
            );
        }

        @DisplayName("PENDING이 아닌 상태에서 complete()를 호출하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenNotPending() {
            // arrange
            Payment payment = new Payment(1L, 10000, "SAMSUNG", "1234");
            payment.complete("TX-001");

            // act & assert
            assertThatThrownBy(() -> payment.complete("TX-002"))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }
    }

    @DisplayName("결제 실패 처리 시,")
    @Nested
    class Fail {

        @DisplayName("PENDING 상태에서 fail()을 호출하면, FAILED로 전이된다.")
        @Test
        void transitionsToFailed_whenPending() {
            // arrange
            Payment payment = new Payment(1L, 10000, "SAMSUNG", "1234");

            // act
            payment.fail("카드 한도 초과");

            // assert
            assertAll(
                () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED),
                () -> assertThat(payment.getFailureReason()).isEqualTo("카드 한도 초과")
            );
        }

        @DisplayName("PENDING이 아닌 상태에서 fail()을 호출하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenNotPending() {
            // arrange
            Payment payment = new Payment(1L, 10000, "SAMSUNG", "1234");
            payment.fail("이미 실패");

            // act & assert
            assertThatThrownBy(() -> payment.fail("다시 실패"))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }
    }
}
