package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class ProductTest {

    private static final Long BRAND_ID = 1L;
    private static final String NAME = "에어맥스";
    private static final String DESCRIPTION = "Nike Air Max";
    private static final int PRICE = 100000;
    private static final int STOCK = 10;
    private static final SellingStatus SELLING_STATUS = SellingStatus.SELLING;

    @DisplayName("상품을 생성할 때,")
    @Nested
    class Create {

        @DisplayName("필수 필드가 정상 제공되면, 정상적으로 생성된다.")
        @Test
        void createsProduct_whenRequiredFieldsAreProvided() {
            // arrange & act
            Product product = new Product(BRAND_ID, NAME, DESCRIPTION, PRICE, STOCK, SELLING_STATUS);

            // assert
            assertAll(
                () -> assertThat(product.getBrandId()).isEqualTo(BRAND_ID),
                () -> assertThat(product.getName()).isEqualTo(NAME),
                () -> assertThat(product.getDescription()).isEqualTo(DESCRIPTION),
                () -> assertThat(product.getPrice()).isEqualTo(PRICE),
                () -> assertThat(product.getStock()).isEqualTo(STOCK),
                () -> assertThat(product.getSellingStatus()).isEqualTo(SELLING_STATUS)
            );
        }

        @DisplayName("이름이 빈값이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenNameIsBlank() {
            // arrange & act & assert
            assertThatThrownBy(() -> new Product(BRAND_ID, "", DESCRIPTION, PRICE, STOCK, SELLING_STATUS))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("brandId가 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenBrandIdIsNull() {
            // arrange & act & assert
            assertThatThrownBy(() -> new Product(null, NAME, DESCRIPTION, PRICE, STOCK, SELLING_STATUS))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("description이 null이면, 정상적으로 생성된다.")
        @Test
        void createsProduct_whenDescriptionIsNull() {
            // arrange & act
            Product product = new Product(BRAND_ID, NAME, null, PRICE, STOCK, SELLING_STATUS);

            // assert
            assertThat(product.getDescription()).isNull();
        }

        @DisplayName("price가 음수이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenPriceIsNegative() {
            // arrange & act & assert
            assertThatThrownBy(() -> new Product(BRAND_ID, NAME, DESCRIPTION, -1, STOCK, SELLING_STATUS))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("stock이 음수이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenStockIsNegative() {
            // arrange & act & assert
            assertThatThrownBy(() -> new Product(BRAND_ID, NAME, DESCRIPTION, PRICE, -1, SELLING_STATUS))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }
    }

    @DisplayName("주문 가능 여부를 확인할 때,")
    @Nested
    class CanOrder {

        @DisplayName("SELLING 상태이고 재고가 0보다 크면, true를 반환한다.")
        @Test
        void returnsTrue_whenSellingAndStockIsPositive() {
            // arrange
            Product product = new Product(BRAND_ID, NAME, DESCRIPTION, PRICE, STOCK, SellingStatus.SELLING);

            // act & assert
            assertThat(product.canOrder()).isTrue();
        }

        @DisplayName("STOP 상태이면, false를 반환한다.")
        @Test
        void returnsFalse_whenStop() {
            // arrange
            Product product = new Product(BRAND_ID, NAME, DESCRIPTION, PRICE, STOCK, SellingStatus.STOP);

            // act & assert
            assertThat(product.canOrder()).isFalse();
        }

        @DisplayName("SOLD_OUT 상태이면, false를 반환한다.")
        @Test
        void returnsFalse_whenSoldOut() {
            // arrange
            Product product = new Product(BRAND_ID, NAME, DESCRIPTION, PRICE, STOCK, SellingStatus.SOLD_OUT);

            // act & assert
            assertThat(product.canOrder()).isFalse();
        }

        @DisplayName("재고가 0이면, false를 반환한다.")
        @Test
        void returnsFalse_whenStockIsZero() {
            // arrange
            Product product = new Product(BRAND_ID, NAME, DESCRIPTION, PRICE, 0, SellingStatus.SELLING);

            // act & assert
            assertThat(product.canOrder()).isFalse();
        }
    }

    @DisplayName("재고를 차감할 때,")
    @Nested
    class DeductStock {

        @DisplayName("재고가 충분하면, 재고가 차감된다.")
        @Test
        void deductsStock_whenStockIsSufficient() {
            // arrange
            Product product = new Product(BRAND_ID, NAME, DESCRIPTION, PRICE, 10, SELLING_STATUS);

            // act
            product.deductStock(3);

            // assert
            assertThat(product.getStock()).isEqualTo(7);
        }

        @DisplayName("재고가 부족하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenStockIsInsufficient() {
            // arrange
            Product product = new Product(BRAND_ID, NAME, DESCRIPTION, PRICE, 5, SELLING_STATUS);

            // act & assert
            assertThatThrownBy(() -> product.deductStock(10))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }
    }

    @DisplayName("재고를 복원할 때,")
    @Nested
    class RestoreStock {

        @DisplayName("정상적으로 재고가 증가한다.")
        @Test
        void restoresStock_whenCalled() {
            // arrange
            Product product = new Product(BRAND_ID, NAME, DESCRIPTION, PRICE, 5, SELLING_STATUS);

            // act
            product.restoreStock(3);

            // assert
            assertThat(product.getStock()).isEqualTo(8);
        }
    }

    @DisplayName("좋아요를 증가할 때,")
    @Nested
    class IncreaseLikes {

        @DisplayName("좋아요 수가 1 증가한다.")
        @Test
        void increasesLikeCount_whenCalled() {
            // arrange
            Product product = new Product(BRAND_ID, NAME, DESCRIPTION, PRICE, STOCK, SELLING_STATUS);

            // act
            product.increaseLikes();

            // assert
            assertThat(product.getLikeCount()).isEqualTo(1);
        }
    }

    @DisplayName("좋아요를 감소할 때,")
    @Nested
    class DecreaseLikes {

        @DisplayName("좋아요 수가 1 감소한다.")
        @Test
        void decreasesLikeCount_whenCalled() {
            // arrange
            Product product = new Product(BRAND_ID, NAME, DESCRIPTION, PRICE, STOCK, SELLING_STATUS);
            product.increaseLikes();

            // act
            product.decreaseLikes();

            // assert
            assertThat(product.getLikeCount()).isEqualTo(0);
        }

        @DisplayName("좋아요 수가 0일 때 감소하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenLikeCountIsZero() {
            // arrange
            Product product = new Product(BRAND_ID, NAME, DESCRIPTION, PRICE, STOCK, SELLING_STATUS);

            // act & assert
            assertThatThrownBy(product::decreaseLikes)
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }
    }

    @DisplayName("상품 정보를 변경할 때,")
    @Nested
    class ChangeProductInfo {

        @DisplayName("유효한 값으로 변경하면, 필드가 업데이트된다.")
        @Test
        void updatesProductInfo_whenValidValuesAreProvided() {
            // arrange
            Product product = new Product(BRAND_ID, NAME, DESCRIPTION, PRICE, STOCK, SELLING_STATUS);
            String newName = "에어조던";
            String newDescription = "Nike Air Jordan";
            int newPrice = 200000;
            SellingStatus newStatus = SellingStatus.STOP;

            // act
            product.changeProductInfo(newName, newDescription, newPrice, newStatus);

            // assert
            assertAll(
                () -> assertThat(product.getName()).isEqualTo(newName),
                () -> assertThat(product.getDescription()).isEqualTo(newDescription),
                () -> assertThat(product.getPrice()).isEqualTo(newPrice),
                () -> assertThat(product.getSellingStatus()).isEqualTo(newStatus),
                () -> assertThat(product.getBrandId()).isEqualTo(BRAND_ID)
            );
        }

        @DisplayName("변경할 이름이 빈값이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenNewNameIsBlank() {
            // arrange
            Product product = new Product(BRAND_ID, NAME, DESCRIPTION, PRICE, STOCK, SELLING_STATUS);

            // act & assert
            assertThatThrownBy(() -> product.changeProductInfo("", DESCRIPTION, PRICE, SELLING_STATUS))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }
    }
}
