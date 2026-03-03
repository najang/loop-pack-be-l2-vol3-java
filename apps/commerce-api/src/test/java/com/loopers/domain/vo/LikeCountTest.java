package com.loopers.domain.vo;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LikeCountTest {

    @DisplayName("LikeCount 생성 시,")
    @Nested
    class Create {

        @DisplayName("0 이상의 값이 주어지면, 정상적으로 생성된다.")
        @Test
        void createsLikeCount_whenValueIsZeroOrPositive() {
            // arrange & act
            LikeCount zero = new LikeCount(0);
            LikeCount positive = new LikeCount(100);

            // assert
            assertThat(zero.getValue()).isEqualTo(0);
            assertThat(positive.getValue()).isEqualTo(100);
        }

        @DisplayName("음수가 주어지면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenValueIsNegative() {
            // arrange & act & assert
            assertThatThrownBy(() -> new LikeCount(-1))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }
    }

    @DisplayName("좋아요 증가 시,")
    @Nested
    class Increase {

        @DisplayName("1 증가된 새 LikeCount를 반환한다.")
        @Test
        void returnsIncreasedLikeCount() {
            // arrange
            LikeCount likeCount = new LikeCount(5);

            // act
            LikeCount result = likeCount.increase();

            // assert
            assertThat(result.getValue()).isEqualTo(6);
        }
    }

    @DisplayName("좋아요 감소 시,")
    @Nested
    class Decrease {

        @DisplayName("1 이상이면, 1 감소된 새 LikeCount를 반환한다.")
        @Test
        void returnsDecreasedLikeCount_whenValueIsPositive() {
            // arrange
            LikeCount likeCount = new LikeCount(5);

            // act
            LikeCount result = likeCount.decrease();

            // assert
            assertThat(result.getValue()).isEqualTo(4);
        }

        @DisplayName("0이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenValueIsZero() {
            // arrange
            LikeCount likeCount = new LikeCount(0);

            // act & assert
            assertThatThrownBy(likeCount::decrease)
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
            LikeCount lc1 = new LikeCount(10);
            LikeCount lc2 = new LikeCount(10);

            // act & assert
            assertThat(lc1).isEqualTo(lc2);
        }
    }
}
