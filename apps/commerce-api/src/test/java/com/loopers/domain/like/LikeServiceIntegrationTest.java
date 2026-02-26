package com.loopers.domain.like;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.SellingStatus;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class LikeServiceIntegrationTest {

    private static final Long BRAND_ID = 1L;
    private static final Long USER_ID = 1L;

    @Autowired
    private LikeService likeService;

    @Autowired
    private ProductService productService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("like() 시,")
    @Nested
    class LikeAction {

        @DisplayName("like 하면 Product.likeCount가 +1된다.")
        @Test
        void increasesLikeCount_whenLiked() {
            // arrange
            Product product = productService.create(BRAND_ID, "에어맥스", "Nike Air Max", 100000, 10, SellingStatus.SELLING);

            // act
            likeService.like(USER_ID, product.getId());

            // assert
            Product updated = productService.findById(product.getId());
            assertThat(updated.getLikeCount()).isEqualTo(1);
        }

        @DisplayName("동일 사용자가 like를 2번 호출하면 likeCount가 1로 유지된다 (멱등).")
        @Test
        void likeCountRemainsOne_whenSameUserLikesTwice() {
            // arrange
            Product product = productService.create(BRAND_ID, "에어맥스", "Nike Air Max", 100000, 10, SellingStatus.SELLING);

            // act
            likeService.like(USER_ID, product.getId());
            likeService.like(USER_ID, product.getId());

            // assert
            Product updated = productService.findById(product.getId());
            assertThat(updated.getLikeCount()).isEqualTo(1);
        }

        @DisplayName("삭제된 상품에 like 시도하면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFound_whenProductIsDeleted() {
            // arrange
            Product product = productService.create(BRAND_ID, "에어맥스", "Nike Air Max", 100000, 10, SellingStatus.SELLING);
            productService.delete(product.getId());

            // act
            CoreException ex = assertThrows(CoreException.class, () -> likeService.like(USER_ID, product.getId()));

            // assert
            assertThat(ex.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }

    @DisplayName("unlike() 시,")
    @Nested
    class Unlike {

        @DisplayName("like 후 unlike 하면 Product.likeCount가 0이 된다.")
        @Test
        void decreasesLikeCount_whenUnliked() {
            // arrange
            Product product = productService.create(BRAND_ID, "에어맥스", "Nike Air Max", 100000, 10, SellingStatus.SELLING);
            likeService.like(USER_ID, product.getId());

            // act
            likeService.unlike(USER_ID, product.getId());

            // assert
            Product updated = productService.findById(product.getId());
            assertThat(updated.getLikeCount()).isEqualTo(0);
        }

        @DisplayName("좋아요 없는 상품에 unlike를 호출해도 likeCount가 0으로 유지된다 (멱등).")
        @Test
        void likeCountRemainsZero_whenUnlikeCalledWithoutLike() {
            // arrange
            Product product = productService.create(BRAND_ID, "에어맥스", "Nike Air Max", 100000, 10, SellingStatus.SELLING);

            // act
            likeService.unlike(USER_ID, product.getId());

            // assert
            Product updated = productService.findById(product.getId());
            assertThat(updated.getLikeCount()).isEqualTo(0);
        }
    }
}
