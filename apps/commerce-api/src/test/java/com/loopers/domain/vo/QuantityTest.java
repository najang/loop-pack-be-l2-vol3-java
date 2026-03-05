package com.loopers.domain.vo;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuantityTest {

    @DisplayName("Quantity 생성 시,")
    @Nested
    class Create {

        @DisplayName("1 이상의 값이 주어지면, 정상적으로 생성된다.")
        @Test
        void createsQuantity_whenValueIsPositive() {
            // arrange & act
            Quantity quantity = new Quantity(5);

            // assert
            assertThat(quantity.getValue()).isEqualTo(5);
        }

        @DisplayName("0이 주어지면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenValueIsZero() {
            // arrange & act & assert
            assertThatThrownBy(() -> new Quantity(0))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("음수가 주어지면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenValueIsNegative() {
            // arrange & act & assert
            assertThatThrownBy(() -> new Quantity(-1))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }
    }

    @DisplayName("수량 합산 시,")
    @Nested
    class Add {

        @DisplayName("다른 Quantity를 더하면, 합산된 새 Quantity를 반환한다.")
        @Test
        void returnsAddedQuantity_whenAddIsCalled() {
            // arrange
            Quantity q1 = new Quantity(3);
            Quantity q2 = new Quantity(5);

            // act
            Quantity result = q1.add(q2);

            // assert
            assertThat(result.getValue()).isEqualTo(8);
        }
    }

    @DisplayName("동등성 비교 시,")
    @Nested
    class Equality {

        @DisplayName("같은 값이면 동등하다.")
        @Test
        void isEqual_whenValueIsSame() {
            // arrange
            Quantity q1 = new Quantity(3);
            Quantity q2 = new Quantity(3);

            // act & assert
            assertThat(q1).isEqualTo(q2);
        }
    }
}
