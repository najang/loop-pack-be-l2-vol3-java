package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class OrderTest {

    private static final Long USER_ID = 1L;
    private static final Long PRODUCT_ID = 100L;

    @DisplayName("Order를 생성할 때,")
    @Nested
    class Create {

        @DisplayName("userId와 items가 정상 제공되면, 정상적으로 생성된다.")
        @Test
        void createsOrder_whenRequiredFieldsAreProvided() {
            // arrange
            List<OrderItem> items = List.of(new OrderItem(PRODUCT_ID, 2, 1000));

            // act
            Order order = new Order(USER_ID, items);

            // assert
            assertAll(
                () -> assertThat(order.getUserId()).isEqualTo(USER_ID),
                () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.ORDERED),
                () -> assertThat(order.getItems()).hasSize(1)
            );
        }

        @DisplayName("totalPrice는 unitPrice * quantity의 합산이다.")
        @Test
        void calculatesTotalPrice_asSumOfUnitPriceTimesQuantity() {
            // arrange
            List<OrderItem> items = List.of(
                new OrderItem(PRODUCT_ID, 2, 1000),
                new OrderItem(200L, 3, 500)
            );

            // act
            Order order = new Order(USER_ID, items);

            // assert
            assertThat(order.getTotalPrice()).isEqualTo(3500);
        }

        @DisplayName("userId가 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenUserIdIsNull() {
            // arrange
            List<OrderItem> items = List.of(new OrderItem(PRODUCT_ID, 1, 1000));

            // act & assert
            assertThatThrownBy(() -> new Order(null, items))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("items가 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenItemsIsNull() {
            // act & assert
            assertThatThrownBy(() -> new Order(USER_ID, null))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("items가 비어 있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenItemsIsEmpty() {
            // act & assert
            assertThatThrownBy(() -> new Order(USER_ID, Collections.emptyList()))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }
    }

    @DisplayName("주문을 취소할 때,")
    @Nested
    class Cancel {

        @DisplayName("ORDERED 상태에서 취소하면, 상태가 CANCELLED로 변경된다.")
        @Test
        void cancelsOrder_whenStatusIsOrdered() {
            // arrange
            Order order = new Order(USER_ID, List.of(new OrderItem(PRODUCT_ID, 1, 1000)));

            // act
            order.cancel();

            // assert
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @DisplayName("SHIPPING 상태에서 취소하면, 상태가 CANCELLED로 변경된다.")
        @Test
        void cancelsOrder_whenStatusIsShipping() {
            // arrange
            Order order = new Order(USER_ID, List.of(new OrderItem(PRODUCT_ID, 1, 1000)));
            order.changeStatus(OrderStatus.SHIPPING);

            // act
            order.cancel();

            // assert
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @DisplayName("DELIVERED 상태에서 취소하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenStatusIsDelivered() {
            // arrange
            Order order = new Order(USER_ID, List.of(new OrderItem(PRODUCT_ID, 1, 1000)));
            order.changeStatus(OrderStatus.DELIVERED);

            // act & assert
            assertThatThrownBy(order::cancel)
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("CANCELLED 상태에서 재취소하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenStatusIsAlreadyCancelled() {
            // arrange
            Order order = new Order(USER_ID, List.of(new OrderItem(PRODUCT_ID, 1, 1000)));
            order.cancel();

            // act & assert
            assertThatThrownBy(order::cancel)
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }
    }
}
