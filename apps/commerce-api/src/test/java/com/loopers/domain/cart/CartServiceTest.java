package com.loopers.domain.cart;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long PRODUCT_ID = 100L;

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private CartService cartService;

    @DisplayName("장바구니 상품 추가 시,")
    @Nested
    class Add {

        @DisplayName("장바구니에 해당 상품이 없으면, 새 Cart를 생성하고 save를 호출한다.")
        @Test
        void savesNewCart_whenCartDoesNotExist() {
            // arrange
            when(cartRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(Optional.empty());
            when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

            // act
            Cart result = cartService.add(USER_ID, PRODUCT_ID, 3);

            // assert
            verify(cartRepository, times(1)).save(any(Cart.class));
            assertThat(result.getQuantity()).isEqualTo(3);
        }

        @DisplayName("장바구니에 해당 상품이 이미 있으면, 수량을 누적하고 save를 호출한다.")
        @Test
        void addsQuantityAndSaves_whenCartAlreadyExists() {
            // arrange
            Cart existingCart = mock(Cart.class);
            when(cartRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(Optional.of(existingCart));
            when(cartRepository.save(existingCart)).thenReturn(existingCart);

            // act
            cartService.add(USER_ID, PRODUCT_ID, 2);

            // assert
            verify(existingCart, times(1)).addQuantity(2);
            verify(cartRepository, times(1)).save(existingCart);
        }
    }

    @DisplayName("장바구니 목록 조회 시,")
    @Nested
    class FindByUserId {

        @DisplayName("해당 user의 장바구니 목록을 반환한다.")
        @Test
        void returnsCartList_forGivenUser() {
            // arrange
            Cart cart1 = mock(Cart.class);
            Cart cart2 = mock(Cart.class);
            when(cartRepository.findByUserId(USER_ID)).thenReturn(List.of(cart1, cart2));

            // act
            List<Cart> result = cartService.findByUserId(USER_ID);

            // assert
            assertThat(result).containsExactly(cart1, cart2);
        }
    }

    @DisplayName("장바구니 수량 변경 시,")
    @Nested
    class UpdateQuantity {

        @DisplayName("해당 항목이 존재하지 않으면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFound_whenCartDoesNotExist() {
            // arrange
            when(cartRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(Optional.empty());

            // act
            CoreException ex = assertThrows(CoreException.class, () -> cartService.updateQuantity(USER_ID, PRODUCT_ID, 5));

            // assert
            assertThat(ex.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
            verify(cartRepository, never()).save(any());
        }

        @DisplayName("해당 항목이 존재하면, cart.updateQuantity()를 호출하고 save한다.")
        @Test
        void updatesQuantityAndSaves_whenCartExists() {
            // arrange
            Cart cart = mock(Cart.class);
            when(cartRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(Optional.of(cart));
            when(cartRepository.save(cart)).thenReturn(cart);

            // act
            cartService.updateQuantity(USER_ID, PRODUCT_ID, 5);

            // assert
            verify(cart, times(1)).updateQuantity(5);
            verify(cartRepository, times(1)).save(cart);
        }
    }

    @DisplayName("장바구니 상품 삭제 시,")
    @Nested
    class Remove {

        @DisplayName("해당 항목이 존재하지 않으면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFound_whenCartDoesNotExist() {
            // arrange
            when(cartRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(Optional.empty());

            // act
            CoreException ex = assertThrows(CoreException.class, () -> cartService.remove(USER_ID, PRODUCT_ID));

            // assert
            assertThat(ex.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
            verify(cartRepository, never()).save(any());
        }

        @DisplayName("해당 항목이 존재하면, cart.delete()를 호출하고 save한다.")
        @Test
        void deletesCartAndSaves_whenCartExists() {
            // arrange
            Cart cart = mock(Cart.class);
            when(cartRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(Optional.of(cart));
            when(cartRepository.save(cart)).thenReturn(cart);

            // act
            cartService.remove(USER_ID, PRODUCT_ID);

            // assert
            verify(cart, times(1)).delete();
            verify(cartRepository, times(1)).save(cart);
        }
    }
}
