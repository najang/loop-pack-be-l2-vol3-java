package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class CouponTemplateTest {

    @DisplayName("CouponTemplate을 생성할 때,")
    @Nested
    class Create {

        @DisplayName("정상 입력이면, 정상 생성되고 isActive=true이다.")
        @Test
        void createsCouponTemplate_whenValidInput() {
            // arrange
            ZonedDateTime expiredAt = ZonedDateTime.now().plusDays(7);

            // act
            CouponTemplate template = new CouponTemplate("여름 할인", CouponType.FIXED, 1000, null, expiredAt);

            // assert
            assertAll(
                () -> assertThat(template.getName()).isEqualTo("여름 할인"),
                () -> assertThat(template.getType()).isEqualTo(CouponType.FIXED),
                () -> assertThat(template.getValue()).isEqualTo(1000),
                () -> assertThat(template.getMinOrderAmount()).isNull(),
                () -> assertThat(template.getExpiredAt()).isEqualTo(expiredAt),
                () -> assertThat(template.isActive()).isTrue()
            );
        }

        @DisplayName("name이 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenNameIsNull() {
            // act & assert
            assertThatThrownBy(() -> new CouponTemplate(null, CouponType.FIXED, 1000, null, ZonedDateTime.now().plusDays(7)))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("name이 blank이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenNameIsBlank() {
            // act & assert
            assertThatThrownBy(() -> new CouponTemplate("  ", CouponType.FIXED, 1000, null, ZonedDateTime.now().plusDays(7)))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("type이 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenTypeIsNull() {
            // act & assert
            assertThatThrownBy(() -> new CouponTemplate("할인", null, 1000, null, ZonedDateTime.now().plusDays(7)))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("value가 0 이하이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenValueIsNotPositive() {
            // act & assert
            assertThatThrownBy(() -> new CouponTemplate("할인", CouponType.FIXED, 0, null, ZonedDateTime.now().plusDays(7)))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("expiredAt이 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenExpiredAtIsNull() {
            // act & assert
            assertThatThrownBy(() -> new CouponTemplate("할인", CouponType.FIXED, 1000, null, null))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }
    }

    @DisplayName("canIssue()를 호출할 때,")
    @Nested
    class CanIssue {

        @DisplayName("isActive=true이고 삭제되지 않았고 만료되지 않았으면, true를 반환한다.")
        @Test
        void returnsTrue_whenActiveAndNotExpired() {
            // arrange
            CouponTemplate template = new CouponTemplate("할인", CouponType.FIXED, 1000, null, ZonedDateTime.now().plusDays(7));

            // act & assert
            assertThat(template.canIssue()).isTrue();
        }

        @DisplayName("isActive=false이면, false를 반환한다.")
        @Test
        void returnsFalse_whenInactive() {
            // arrange
            CouponTemplate template = new CouponTemplate("할인", CouponType.FIXED, 1000, null, ZonedDateTime.now().plusDays(7));
            template.deactivate();

            // act & assert
            assertThat(template.canIssue()).isFalse();
        }

        @DisplayName("만료 시간이 지났으면, false를 반환한다.")
        @Test
        void returnsFalse_whenExpired() {
            // arrange
            CouponTemplate template = new CouponTemplate("할인", CouponType.FIXED, 1000, null, ZonedDateTime.now().minusDays(1));

            // act & assert
            assertThat(template.canIssue()).isFalse();
        }
    }

    @DisplayName("calculateDiscount()를 호출할 때,")
    @Nested
    class CalculateDiscount {

        @DisplayName("FIXED 타입이면, value 그대로 반환된다.")
        @Test
        void returnsFixedValue_whenTypeIsFixed() {
            // arrange
            CouponTemplate template = new CouponTemplate("정액 할인", CouponType.FIXED, 3000, null, ZonedDateTime.now().plusDays(7));

            // act
            int discount = template.calculateDiscount(50000);

            // assert
            assertThat(discount).isEqualTo(3000);
        }

        @DisplayName("RATE 타입이면, 금액의 value%가 반환된다.")
        @Test
        void returnsPercentageValue_whenTypeIsRate() {
            // arrange
            CouponTemplate template = new CouponTemplate("10% 할인", CouponType.RATE, 10, null, ZonedDateTime.now().plusDays(7));

            // act
            int discount = template.calculateDiscount(50000);

            // assert
            assertThat(discount).isEqualTo(5000);
        }

        @DisplayName("FIXED 타입 할인액이 주문 금액을 초과하면, 주문 금액만큼만 반환된다.")
        @Test
        void returnsCappedDiscount_whenFixedExceedsAmount() {
            // arrange
            CouponTemplate template = new CouponTemplate("정액 할인", CouponType.FIXED, 10000, null, ZonedDateTime.now().plusDays(7));

            // act
            int discount = template.calculateDiscount(5000);

            // assert
            assertThat(discount).isEqualTo(5000);
        }
    }

    @DisplayName("update()를 호출할 때,")
    @Nested
    class Update {

        @DisplayName("정상 입력이면, 필드가 업데이트된다.")
        @Test
        void updatesFields_whenValidInput() {
            // arrange
            CouponTemplate template = new CouponTemplate("기존명", CouponType.FIXED, 1000, null, ZonedDateTime.now().plusDays(7));
            ZonedDateTime newExpiredAt = ZonedDateTime.now().plusDays(30);

            // act
            template.update("새이름", CouponType.RATE, 20, 10000, newExpiredAt);

            // assert
            assertAll(
                () -> assertThat(template.getName()).isEqualTo("새이름"),
                () -> assertThat(template.getType()).isEqualTo(CouponType.RATE),
                () -> assertThat(template.getValue()).isEqualTo(20),
                () -> assertThat(template.getMinOrderAmount()).isEqualTo(10000),
                () -> assertThat(template.getExpiredAt()).isEqualTo(newExpiredAt)
            );
        }
    }
}
