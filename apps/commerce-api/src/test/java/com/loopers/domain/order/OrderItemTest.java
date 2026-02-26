package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class OrderItemTest {

    private static final Long PRODUCT_ID = 100L;
    private static final String PRODUCT_NAME = "테스트 상품";
    private static final String BRAND_NAME = "테스트 브랜드";

    @DisplayName("OrderItem을 생성할 때,")
    @Nested
    class Create {

        @DisplayName("productId, productName, brandName, quantity, unitPrice가 정상 제공되면, 정상적으로 생성된다.")
        @Test
        void createsOrderItem_whenRequiredFieldsAreProvided() {
            // arrange & act
            OrderItem item = new OrderItem(PRODUCT_ID, PRODUCT_NAME, BRAND_NAME, 2, 1000);

            // assert
            assertAll(
                () -> assertThat(item.getProductId()).isEqualTo(PRODUCT_ID),
                () -> assertThat(item.getProductName()).isEqualTo(PRODUCT_NAME),
                () -> assertThat(item.getBrandName()).isEqualTo(BRAND_NAME),
                () -> assertThat(item.getQuantity()).isEqualTo(2),
                () -> assertThat(item.getUnitPrice()).isEqualTo(1000)
            );
        }

        @DisplayName("productId가 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenProductIdIsNull() {
            // arrange & act & assert
            assertThatThrownBy(() -> new OrderItem(null, PRODUCT_NAME, BRAND_NAME, 2, 1000))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("quantity가 1 미만이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenQuantityIsLessThanOne() {
            // arrange & act & assert
            assertThatThrownBy(() -> new OrderItem(PRODUCT_ID, PRODUCT_NAME, BRAND_NAME, 0, 1000))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("unitPrice가 0 미만이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenUnitPriceIsNegative() {
            // arrange & act & assert
            assertThatThrownBy(() -> new OrderItem(PRODUCT_ID, PRODUCT_NAME, BRAND_NAME, 2, -1))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }
    }
}
