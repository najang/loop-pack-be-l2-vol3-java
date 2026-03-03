package com.loopers.domain.cart;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class CartServiceIntegrationTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long PRODUCT_ID = 100L;
    private static final Long ANOTHER_PRODUCT_ID = 200L;

    @Autowired
    private CartService cartService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("장바구니 상품 추가 시,")
    @Nested
    class Add {

        @DisplayName("신규 상품이면, Cart 항목이 생성된다.")
        @Test
        void createsNewCartItem_whenProductIsNew() {
            // act
            Cart cart = cartService.add(USER_ID, PRODUCT_ID, 3);

            // assert
            assertThat(cart.getUserId()).isEqualTo(USER_ID);
            assertThat(cart.getProductId()).isEqualTo(PRODUCT_ID);
            assertThat(cart.getQuantity()).isEqualTo(3);
        }

        @DisplayName("이미 장바구니에 있는 상품이면, 수량이 누적된다.")
        @Test
        void accumulatesQuantity_whenProductAlreadyInCart() {
            // arrange
            cartService.add(USER_ID, PRODUCT_ID, 3);

            // act
            Cart result = cartService.add(USER_ID, PRODUCT_ID, 2);

            // assert
            assertThat(result.getQuantity()).isEqualTo(5);
        }
    }

    @DisplayName("장바구니 목록 조회 시,")
    @Nested
    class FindByUserId {

        @DisplayName("해당 user의 장바구니 목록을 반환한다.")
        @Test
        void returnsCartList_forGivenUser() {
            // arrange
            cartService.add(USER_ID, PRODUCT_ID, 1);
            cartService.add(USER_ID, ANOTHER_PRODUCT_ID, 2);
            cartService.add(OTHER_USER_ID, PRODUCT_ID, 1);

            // act
            List<Cart> result = cartService.findByUserId(USER_ID);

            // assert
            assertThat(result).hasSize(2);
            assertThat(result).allMatch(cart -> cart.getUserId().equals(USER_ID));
        }

        @DisplayName("삭제된 항목은 목록에서 제외된다.")
        @Test
        void excludesDeletedItems_fromList() {
            // arrange
            cartService.add(USER_ID, PRODUCT_ID, 1);
            cartService.add(USER_ID, ANOTHER_PRODUCT_ID, 2);
            cartService.remove(USER_ID, PRODUCT_ID);

            // act
            List<Cart> result = cartService.findByUserId(USER_ID);

            // assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getProductId()).isEqualTo(ANOTHER_PRODUCT_ID);
        }
    }

    @DisplayName("장바구니 수량 변경 시,")
    @Nested
    class UpdateQuantity {

        @DisplayName("정상적으로 수량을 변경할 수 있다.")
        @Test
        void updatesQuantity_successfully() {
            // arrange
            cartService.add(USER_ID, PRODUCT_ID, 3);

            // act
            Cart result = cartService.updateQuantity(USER_ID, PRODUCT_ID, 10);

            // assert
            assertThat(result.getQuantity()).isEqualTo(10);
        }

        @DisplayName("존재하지 않는 항목 수량 변경 시, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFound_whenCartItemDoesNotExist() {
            // act
            CoreException ex = assertThrows(CoreException.class, () -> cartService.updateQuantity(USER_ID, PRODUCT_ID, 5));

            // assert
            assertThat(ex.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }

    @DisplayName("장바구니 상품 삭제 시,")
    @Nested
    class Remove {

        @DisplayName("삭제 후 findByUserId 목록에서 제외된다.")
        @Test
        void removesCartItem_fromList() {
            // arrange
            cartService.add(USER_ID, PRODUCT_ID, 1);

            // act
            cartService.remove(USER_ID, PRODUCT_ID);

            // assert
            List<Cart> result = cartService.findByUserId(USER_ID);
            assertThat(result).isEmpty();
        }

        @DisplayName("존재하지 않는 항목 삭제 시, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFound_whenCartItemDoesNotExist() {
            // act
            CoreException ex = assertThrows(CoreException.class, () -> cartService.remove(USER_ID, PRODUCT_ID));

            // assert
            assertThat(ex.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }
}
