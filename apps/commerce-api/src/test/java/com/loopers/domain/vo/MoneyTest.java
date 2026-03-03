package com.loopers.domain.vo;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @DisplayName("Money 생성 시,")
    @Nested
    class Create {

        @DisplayName("0 이상의 값이 주어지면, 정상적으로 생성된다.")
        @Test
        void createsMoney_whenValueIsZeroOrPositive() {
            // arrange & act
            Money zero = new Money(0);
            Money positive = new Money(10000);

            // assert
            assertThat(zero.getValue()).isEqualTo(0);
            assertThat(positive.getValue()).isEqualTo(10000);
        }

        @DisplayName("음수가 주어지면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenValueIsNegative() {
            // arrange & act & assert
            assertThatThrownBy(() -> new Money(-1))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }
    }

    @DisplayName("동등성 비교 시,")
    @Nested
    class Equality {

        @DisplayName("같은 값이면 동등하다.")
        @Test
        void isEqual_whenValueIsSame() {
            // arrange
            Money money1 = new Money(1000);
            Money money2 = new Money(1000);

            // act & assert
            assertThat(money1).isEqualTo(money2);
        }

        @DisplayName("다른 값이면 동등하지 않다.")
        @Test
        void isNotEqual_whenValueIsDifferent() {
            // arrange
            Money money1 = new Money(1000);
            Money money2 = new Money(2000);

            // act & assert
            assertThat(money1).isNotEqualTo(money2);
        }
    }
}
