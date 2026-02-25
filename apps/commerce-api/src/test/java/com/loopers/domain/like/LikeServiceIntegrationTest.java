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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class LikeServiceIntegrationTest {

    private static final Long BRAND_ID = 1L;
    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

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

    @DisplayName("좋아요 토글 시,")
    @Nested
    class Toggle {

        @DisplayName("toggle 하면 Product.likeCount가 +1된다.")
        @Test
        void increasesLikeCount_whenToggleOn() {
            // arrange
            Product product = productService.create(BRAND_ID, "에어맥스", "Nike Air Max", 100000, 10, SellingStatus.SELLING);

            // act
            likeService.toggle(USER_ID, product.getId());

            // assert
            Product updated = productService.findById(product.getId());
            assertThat(updated.getLikeCount()).isEqualTo(1);
        }

        @DisplayName("toggle 후 다시 toggle하면 Product.likeCount가 -1된다.")
        @Test
        void decreasesLikeCount_whenToggleOff() {
            // arrange
            Product product = productService.create(BRAND_ID, "에어맥스", "Nike Air Max", 100000, 10, SellingStatus.SELLING);
            likeService.toggle(USER_ID, product.getId());

            // act
            likeService.toggle(USER_ID, product.getId());

            // assert
            Product updated = productService.findById(product.getId());
            assertThat(updated.getLikeCount()).isEqualTo(0);
        }

        @DisplayName("삭제된 상품에 toggle 시도하면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFound_whenProductIsDeleted() {
            // arrange
            Product product = productService.create(BRAND_ID, "에어맥스", "Nike Air Max", 100000, 10, SellingStatus.SELLING);
            productService.delete(product.getId());

            // act
            CoreException ex = assertThrows(CoreException.class, () -> likeService.toggle(USER_ID, product.getId()));

            // assert
            assertThat(ex.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }

    @DisplayName("좋아요한 상품 ID 목록 조회 시,")
    @Nested
    class FindLikedProductIds {

        @DisplayName("해당 user의 좋아요한 productId 목록을 반환한다.")
        @Test
        void returnsLikedProductIds_forGivenUser() {
            // arrange
            Product product1 = productService.create(BRAND_ID, "에어맥스", "Nike Air Max", 100000, 10, SellingStatus.SELLING);
            Product product2 = productService.create(BRAND_ID, "에어조던", "Nike Air Jordan", 200000, 5, SellingStatus.SELLING);
            likeService.toggle(USER_ID, product1.getId());
            likeService.toggle(USER_ID, product2.getId());

            // act
            List<Long> result = likeService.findLikedProductIds(USER_ID);

            // assert
            assertThat(result).containsExactlyInAnyOrder(product1.getId(), product2.getId());
        }

        @DisplayName("다른 user의 좋아요는 포함되지 않는다.")
        @Test
        void excludesOtherUsersLikes() {
            // arrange
            Product product1 = productService.create(BRAND_ID, "에어맥스", "Nike Air Max", 100000, 10, SellingStatus.SELLING);
            Product product2 = productService.create(BRAND_ID, "에어조던", "Nike Air Jordan", 200000, 5, SellingStatus.SELLING);
            likeService.toggle(USER_ID, product1.getId());
            likeService.toggle(OTHER_USER_ID, product2.getId());

            // act
            List<Long> result = likeService.findLikedProductIds(USER_ID);

            // assert
            assertThat(result).containsExactly(product1.getId());
            assertThat(result).doesNotContain(product2.getId());
        }
    }
}
