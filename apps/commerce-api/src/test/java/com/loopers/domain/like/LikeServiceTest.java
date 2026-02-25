package com.loopers.domain.like;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.SellingStatus;
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
class LikeServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long PRODUCT_ID = 100L;

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private LikeService likeService;

    @DisplayName("좋아요 토글 시,")
    @Nested
    class Toggle {

        @DisplayName("좋아요가 없으면, save 후 product.increaseLikes() + productRepository.save() 호출한다.")
        @Test
        void savesLikeAndIncreasesLikeCount_whenLikeDoesNotExist() {
            // arrange
            Product product = mock(Product.class);
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
            when(likeRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(Optional.empty());
            when(likeRepository.save(any(Like.class))).thenAnswer(inv -> inv.getArgument(0));
            when(productRepository.save(product)).thenReturn(product);

            // act
            likeService.toggle(USER_ID, PRODUCT_ID);

            // assert
            verify(likeRepository, times(1)).save(any(Like.class));
            verify(product, times(1)).increaseLikes();
            verify(productRepository, times(1)).save(product);
        }

        @DisplayName("좋아요가 있으면, delete 후 product.decreaseLikes() + productRepository.save() 호출한다.")
        @Test
        void deletesLikeAndDecreasesLikeCount_whenLikeExists() {
            // arrange
            Product product = mock(Product.class);
            Like like = mock(Like.class);
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
            when(likeRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(Optional.of(like));
            when(productRepository.save(product)).thenReturn(product);

            // act
            likeService.toggle(USER_ID, PRODUCT_ID);

            // assert
            verify(likeRepository, times(1)).delete(like);
            verify(product, times(1)).decreaseLikes();
            verify(productRepository, times(1)).save(product);
        }

        @DisplayName("상품이 존재하지 않으면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFound_whenProductDoesNotExist() {
            // arrange
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

            // act
            CoreException ex = assertThrows(CoreException.class, () -> likeService.toggle(USER_ID, PRODUCT_ID));

            // assert
            assertThat(ex.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
            verify(likeRepository, never()).save(any());
            verify(likeRepository, never()).delete(any());
        }
    }

    @DisplayName("좋아요한 상품 ID 목록 조회 시,")
    @Nested
    class FindLikedProductIds {

        @DisplayName("해당 user의 좋아요 목록으로 productId 목록을 반환한다.")
        @Test
        void returnsProductIds_forGivenUser() {
            // arrange
            Like like1 = mock(Like.class);
            Like like2 = mock(Like.class);
            when(like1.getProductId()).thenReturn(100L);
            when(like2.getProductId()).thenReturn(200L);
            when(likeRepository.findByUserId(USER_ID)).thenReturn(List.of(like1, like2));

            // act
            List<Long> result = likeService.findLikedProductIds(USER_ID);

            // assert
            assertThat(result).containsExactlyInAnyOrder(100L, 200L);
        }
    }
}
