package com.loopers.domain.like;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class LikeTest {

    private static final Long USER_ID = 1L;
    private static final Long PRODUCT_ID = 100L;

    @DisplayName("좋아요를 생성할 때,")
    @Nested
    class Create {

        @DisplayName("userId와 productId가 정상 제공되면, 정상적으로 생성된다.")
        @Test
        void createsLike_whenRequiredFieldsAreProvided() {
            // arrange & act
            Like like = new Like(USER_ID, PRODUCT_ID);

            // assert
            assertAll(
                () -> assertThat(like.getUserId()).isEqualTo(USER_ID),
                () -> assertThat(like.getProductId()).isEqualTo(PRODUCT_ID)
            );
        }

        @DisplayName("userId가 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenUserIdIsNull() {
            // arrange & act & assert
            assertThatThrownBy(() -> new Like(null, PRODUCT_ID))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("productId가 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenProductIdIsNull() {
            // arrange & act & assert
            assertThatThrownBy(() -> new Like(USER_ID, null))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }
    }
}
