package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class ProductServiceIntegrationTest {

    private static final Long BRAND_ID = 1L;
    private static final String NAME = "에어맥스";
    private static final String DESCRIPTION = "Nike Air Max";
    private static final int PRICE = 100000;
    private static final int STOCK = 10;
    private static final SellingStatus SELLING_STATUS = SellingStatus.SELLING;

    @Autowired
    private ProductService productService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("상품 생성 후 조회 시,")
    @Nested
    class CreateAndFind {

        @DisplayName("create 후 findById로 조회 성공한다.")
        @Test
        void findsProduct_afterCreate() {
            // arrange
            Product created = productService.create(BRAND_ID, NAME, DESCRIPTION, PRICE, STOCK, SELLING_STATUS);

            // act
            Product found = productService.findById(created.getId());

            // assert
            assertAll(
                () -> assertThat(found.getId()).isEqualTo(created.getId()),
                () -> assertThat(found.getName()).isEqualTo(NAME),
                () -> assertThat(found.getDescription()).isEqualTo(DESCRIPTION),
                () -> assertThat(found.getPrice()).isEqualTo(PRICE),
                () -> assertThat(found.getStock()).isEqualTo(STOCK),
                () -> assertThat(found.getSellingStatus()).isEqualTo(SELLING_STATUS)
            );
        }
    }

    @DisplayName("상품 삭제 시,")
    @Nested
    class Delete {

        @DisplayName("delete 후 findById → NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFound_afterDelete() {
            // arrange
            Product created = productService.create(BRAND_ID, NAME, DESCRIPTION, PRICE, STOCK, SELLING_STATUS);
            productService.delete(created.getId());

            // act
            CoreException ex = assertThrows(CoreException.class, () -> productService.findById(created.getId()));

            // assert
            assertThat(ex.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }

    @DisplayName("상품 수정 시,")
    @Nested
    class Update {

        @DisplayName("update 후 findById → 변경된 값을 확인한다.")
        @Test
        void returnsUpdatedProduct_afterUpdate() {
            // arrange
            Product created = productService.create(BRAND_ID, NAME, DESCRIPTION, PRICE, STOCK, SELLING_STATUS);
            String newName = "에어조던";
            String newDescription = "Nike Air Jordan";
            int newPrice = 200000;
            SellingStatus newStatus = SellingStatus.STOP;

            // act
            productService.update(created.getId(), newName, newDescription, newPrice, newStatus);
            Product updated = productService.findById(created.getId());

            // assert
            assertAll(
                () -> assertThat(updated.getName()).isEqualTo(newName),
                () -> assertThat(updated.getDescription()).isEqualTo(newDescription),
                () -> assertThat(updated.getPrice()).isEqualTo(newPrice),
                () -> assertThat(updated.getSellingStatus()).isEqualTo(newStatus)
            );
        }
    }

    @DisplayName("상품 전체 조회 시,")
    @Nested
    class FindAll {

        @DisplayName("삭제된 상품은 제외하고 반환한다.")
        @Test
        void excludesDeletedProducts_whenFindAll() {
            // arrange
            Product active = productService.create(BRAND_ID, NAME, DESCRIPTION, PRICE, STOCK, SELLING_STATUS);
            Product toDelete = productService.create(BRAND_ID, "에어조던", "Nike Air Jordan", 200000, 5, SELLING_STATUS);
            productService.delete(toDelete.getId());

            // act
            Page<Product> result = productService.findAll(null, PageRequest.of(0, 10));

            // assert
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(active.getId());
        }

        @DisplayName("brandId 필터를 지정하면, 해당 브랜드 상품만 반환한다.")
        @Test
        void returnsOnlyBrandProducts_whenBrandIdIsProvided() {
            // arrange
            Long otherBrandId = 2L;
            productService.create(BRAND_ID, NAME, DESCRIPTION, PRICE, STOCK, SELLING_STATUS);
            productService.create(BRAND_ID, "에어포스", "Nike Air Force", 120000, 8, SELLING_STATUS);
            productService.create(otherBrandId, "울트라부스트", "Adidas Ultra Boost", 180000, 5, SELLING_STATUS);

            // act
            Page<Product> result = productService.findAll(BRAND_ID, PageRequest.of(0, 10));

            // assert
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).allMatch(p -> p.getBrandId().equals(BRAND_ID));
        }
    }
}
