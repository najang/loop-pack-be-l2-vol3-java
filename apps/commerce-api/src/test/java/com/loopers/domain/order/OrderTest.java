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

        @DisplayName("userId와 items가 정상 제공되면, PAYMENT_PENDING 상태로 생성된다.")
        @Test
        void createsOrder_withPaymentPendingStatus() {
            // arrange
            List<OrderItem> items = List.of(new OrderItem(PRODUCT_ID, "상품명", "브랜드명", 2, 1000));

            // act
            Order order = new Order(USER_ID, items);

            // assert
            assertAll(
                () -> assertThat(order.getUserId()).isEqualTo(USER_ID),
                () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING),
                () -> assertThat(order.getItems()).hasSize(1)
            );
        }

        @DisplayName("쿠폰 없이 생성하면, originalTotalPrice=합산, discountAmount=0, finalTotalPrice=합산이다.")
        @Test
        void calculatesTotalPrice_asSumOfUnitPriceTimesQuantity() {
            // arrange
            List<OrderItem> items = List.of(
                new OrderItem(PRODUCT_ID, "상품명", "브랜드명", 2, 1000),
                new OrderItem(200L, "다른상품", "다른브랜드", 3, 500)
            );

            // act
            Order order = new Order(USER_ID, items);

            // assert
            assertAll(
                () -> assertThat(order.getOriginalTotalPrice()).isEqualTo(3500),
                () -> assertThat(order.getDiscountAmount()).isEqualTo(0),
                () -> assertThat(order.getFinalTotalPrice()).isEqualTo(3500),
                () -> assertThat(order.getUserCouponId()).isNull()
            );
        }

        @DisplayName("쿠폰을 적용하면, discountAmount와 finalTotalPrice가 올바르게 계산된다.")
        @Test
        void calculatesPricesWithCoupon() {
            // arrange
            List<OrderItem> items = List.of(new OrderItem(PRODUCT_ID, "상품명", "브랜드명", 2, 10000));
            Long userCouponId = 5L;
            int discountAmount = 3000;

            // act
            Order order = new Order(USER_ID, items, userCouponId, discountAmount);

            // assert
            assertAll(
                () -> assertThat(order.getOriginalTotalPrice()).isEqualTo(20000),
                () -> assertThat(order.getDiscountAmount()).isEqualTo(3000),
                () -> assertThat(order.getFinalTotalPrice()).isEqualTo(17000),
                () -> assertThat(order.getUserCouponId()).isEqualTo(userCouponId)
            );
        }

        @DisplayName("userId가 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenUserIdIsNull() {
            // arrange
            List<OrderItem> items = List.of(new OrderItem(PRODUCT_ID, "상품명", "브랜드명", 1, 1000));

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

    @DisplayName("결제 완료 처리 시,")
    @Nested
    class ConfirmPayment {

        @DisplayName("PAYMENT_PENDING 상태에서 confirmPayment()를 호출하면, ORDERED로 전이된다.")
        @Test
        void transitionsToOrdered_whenPaymentPending() {
            // arrange
            Order order = new Order(USER_ID, List.of(new OrderItem(PRODUCT_ID, "상품명", "브랜드명", 1, 1000)));

            // act
            order.confirmPayment();

            // assert
            assertThat(order.getStatus()).isEqualTo(OrderStatus.ORDERED);
        }

        @DisplayName("PAYMENT_PENDING이 아닌 상태에서 confirmPayment()를 호출하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenNotPaymentPending() {
            // arrange
            Order order = new Order(USER_ID, List.of(new OrderItem(PRODUCT_ID, "상품명", "브랜드명", 1, 1000)));
            order.confirmPayment();

            // act & assert
            assertThatThrownBy(order::confirmPayment)
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }
    }

    @DisplayName("결제 실패 처리 시,")
    @Nested
    class FailPayment {

        @DisplayName("PAYMENT_PENDING 상태에서 failPayment()를 호출하면, PAYMENT_FAILED로 전이된다.")
        @Test
        void transitionsToPaymentFailed_whenPaymentPending() {
            // arrange
            Order order = new Order(USER_ID, List.of(new OrderItem(PRODUCT_ID, "상품명", "브랜드명", 1, 1000)));

            // act
            order.failPayment();

            // assert
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        }

        @DisplayName("PAYMENT_PENDING이 아닌 상태에서 failPayment()를 호출하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenNotPaymentPending() {
            // arrange
            Order order = new Order(USER_ID, List.of(new OrderItem(PRODUCT_ID, "상품명", "브랜드명", 1, 1000)));
            order.failPayment();

            // act & assert
            assertThatThrownBy(order::failPayment)
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
            Order order = new Order(USER_ID, List.of(new OrderItem(PRODUCT_ID, "상품명", "브랜드명", 1, 1000)));
            order.confirmPayment();

            // act
            order.cancel();

            // assert
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @DisplayName("SHIPPING 상태에서 취소하면, 상태가 CANCELLED로 변경된다.")
        @Test
        void cancelsOrder_whenStatusIsShipping() {
            // arrange
            Order order = new Order(USER_ID, List.of(new OrderItem(PRODUCT_ID, "상품명", "브랜드명", 1, 1000)));
            order.changeStatus(OrderStatus.SHIPPING);

            // act
            order.cancel();

            // assert
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @DisplayName("PAYMENT_PENDING 상태에서 취소하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenStatusIsPaymentPending() {
            // arrange
            Order order = new Order(USER_ID, List.of(new OrderItem(PRODUCT_ID, "상품명", "브랜드명", 1, 1000)));

            // act & assert
            assertThatThrownBy(order::cancel)
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("DELIVERED 상태에서 취소하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenStatusIsDelivered() {
            // arrange
            Order order = new Order(USER_ID, List.of(new OrderItem(PRODUCT_ID, "상품명", "브랜드명", 1, 1000)));
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
            Order order = new Order(USER_ID, List.of(new OrderItem(PRODUCT_ID, "상품명", "브랜드명", 1, 1000)));
            order.confirmPayment();
            order.cancel();

            // act & assert
            assertThatThrownBy(order::cancel)
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }
    }
}
