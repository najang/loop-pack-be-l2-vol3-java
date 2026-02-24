package com.loopers.domain.vo;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockTest {

    @DisplayName("Stock 생성 시,")
    @Nested
    class Create {

        @DisplayName("0 이상의 값이 주어지면, 정상적으로 생성된다.")
        @Test
        void createsStock_whenValueIsZeroOrPositive() {
            // arrange & act
            Stock zero = new Stock(0);
            Stock positive = new Stock(100);

            // assert
            assertThat(zero.getValue()).isEqualTo(0);
            assertThat(positive.getValue()).isEqualTo(100);
        }

        @DisplayName("음수가 주어지면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenValueIsNegative() {
            // arrange & act & assert
            assertThatThrownBy(() -> new Stock(-1))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }
    }

    @DisplayName("재고 차감 시,")
    @Nested
    class Deduct {

        @DisplayName("차감 후 재고가 0 이상이면, 차감된 새 Stock을 반환한다.")
        @Test
        void returnsDeductedStock_whenSufficientStock() {
            // arrange
            Stock stock = new Stock(10);

            // act
            Stock result = stock.deduct(3);

            // assert
            assertThat(result.getValue()).isEqualTo(7);
        }

        @DisplayName("전체 재고를 차감하면, 0인 Stock을 반환한다.")
        @Test
        void returnsZeroStock_whenDeductAll() {
            // arrange
            Stock stock = new Stock(5);

            // act
            Stock result = stock.deduct(5);

            // assert
            assertThat(result.getValue()).isEqualTo(0);
        }

        @DisplayName("재고보다 많은 수량을 차감하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenInsufficientStock() {
            // arrange
            Stock stock = new Stock(3);

            // act & assert
            assertThatThrownBy(() -> stock.deduct(5))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }
    }

    @DisplayName("재고 복원 시,")
    @Nested
    class Restore {

        @DisplayName("양수를 복원하면, 증가된 새 Stock을 반환한다.")
        @Test
        void returnsRestoredStock_whenPositiveAmount() {
            // arrange
            Stock stock = new Stock(5);

            // act
            Stock result = stock.restore(3);

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
            Stock s1 = new Stock(10);
            Stock s2 = new Stock(10);

            // act & assert
            assertThat(s1).isEqualTo(s2);
        }
    }
}
