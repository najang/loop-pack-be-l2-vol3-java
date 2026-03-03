package com.loopers.domain.cart;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class CartTest {

    private static final Long USER_ID = 1L;
    private static final Long PRODUCT_ID = 100L;

    @DisplayName("장바구니를 생성할 때,")
    @Nested
    class Create {

        @DisplayName("userId, productId, quantity가 정상 제공되면, 정상적으로 생성된다.")
        @Test
        void createsCart_whenRequiredFieldsAreProvided() {
            // arrange & act
            Cart cart = new Cart(USER_ID, PRODUCT_ID, 3);

            // assert
            assertAll(
                () -> assertThat(cart.getUserId()).isEqualTo(USER_ID),
                () -> assertThat(cart.getProductId()).isEqualTo(PRODUCT_ID),
                () -> assertThat(cart.getQuantity()).isEqualTo(3)
            );
        }

        @DisplayName("userId가 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenUserIdIsNull() {
            // arrange & act & assert
            assertThatThrownBy(() -> new Cart(null, PRODUCT_ID, 1))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("productId가 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenProductIdIsNull() {
            // arrange & act & assert
            assertThatThrownBy(() -> new Cart(USER_ID, null, 1))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("quantity가 1 미만이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenQuantityIsLessThanOne() {
            // arrange & act & assert
            assertThatThrownBy(() -> new Cart(USER_ID, PRODUCT_ID, 0))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }
    }

    @DisplayName("수량을 추가할 때,")
    @Nested
    class AddQuantity {

        @DisplayName("기존 수량에 추가 수량을 더한 값으로 증가한다.")
        @Test
        void addsQuantity_toExistingQuantity() {
            // arrange
            Cart cart = new Cart(USER_ID, PRODUCT_ID, 3);

            // act
            cart.addQuantity(2);

            // assert
            assertThat(cart.getQuantity()).isEqualTo(5);
        }
    }

    @DisplayName("수량을 변경할 때,")
    @Nested
    class UpdateQuantity {

        @DisplayName("1 미만의 수량으로 변경하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenQuantityIsLessThanOne() {
            // arrange
            Cart cart = new Cart(USER_ID, PRODUCT_ID, 3);

            // act & assert
            assertThatThrownBy(() -> cart.updateQuantity(0))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("정상 수량으로 변경하면, 수량이 갱신된다.")
        @Test
        void updatesQuantity_toNewValue() {
            // arrange
            Cart cart = new Cart(USER_ID, PRODUCT_ID, 3);

            // act
            cart.updateQuantity(7);

            // assert
            assertThat(cart.getQuantity()).isEqualTo(7);
        }
    }
}
