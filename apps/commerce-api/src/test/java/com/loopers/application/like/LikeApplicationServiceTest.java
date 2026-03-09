package com.loopers.application.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class LikeApplicationServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long PRODUCT_ID = 100L;

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private LikeApplicationService likeApplicationService;

    @DisplayName("like() 시,")
    @Nested
    class LikeAction {

        @DisplayName("좋아요가 없으면, save 후 product.increaseLikes() + productRepository.save() 호출한다.")
        @Test
        void savesLikeAndIncreasesLikeCount_whenLikeDoesNotExist() {
            // arrange
            Product product = mock(Product.class);
            when(productRepository.findByIdWithLock(PRODUCT_ID)).thenReturn(Optional.of(product));
            when(likeRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(Optional.empty());
            when(likeRepository.save(any(Like.class))).thenAnswer(inv -> inv.getArgument(0));
            when(productRepository.save(product)).thenReturn(product);

            // act
            likeApplicationService.like(USER_ID, PRODUCT_ID);

            // assert
            verify(likeRepository, times(1)).save(any(Like.class));
            verify(product, times(1)).increaseLikes();
            verify(productRepository, times(1)).save(product);
        }

        @DisplayName("좋아요가 이미 있으면, save/increaseLikes/productRepository.save를 호출하지 않는다 (멱등).")
        @Test
        void doesNotSaveOrIncrease_whenLikeAlreadyExists() {
            // arrange
            Product product = mock(Product.class);
            Like like = mock(Like.class);
            when(productRepository.findByIdWithLock(PRODUCT_ID)).thenReturn(Optional.of(product));
            when(likeRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(Optional.of(like));

            // act
            likeApplicationService.like(USER_ID, PRODUCT_ID);

            // assert
            verify(likeRepository, never()).save(any());
            verify(product, never()).increaseLikes();
            verify(productRepository, never()).save(any());
        }

        @DisplayName("상품이 존재하지 않으면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFound_whenProductDoesNotExist() {
            // arrange
            when(productRepository.findByIdWithLock(PRODUCT_ID)).thenReturn(Optional.empty());

            // act
            CoreException ex = assertThrows(CoreException.class, () -> likeApplicationService.like(USER_ID, PRODUCT_ID));

            // assert
            assertThat(ex.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
            verify(likeRepository, never()).save(any());
        }
    }

    @DisplayName("unlike() 시,")
    @Nested
    class Unlike {

        @DisplayName("좋아요가 있으면, delete 후 product.decreaseLikes() + productRepository.save() 호출하고 product를 반환한다.")
        @Test
        void deletesLikeAndDecreasesLikeCount_whenLikeExists() {
            // arrange
            Product product = mock(Product.class);
            Like like = mock(Like.class);
            when(productRepository.findByIdWithLock(PRODUCT_ID)).thenReturn(Optional.of(product));
            when(likeRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(Optional.of(like));
            when(productRepository.save(product)).thenReturn(product);

            // act
            Product result = likeApplicationService.unlike(USER_ID, PRODUCT_ID);

            // assert
            verify(likeRepository, times(1)).delete(like);
            verify(product, times(1)).decreaseLikes();
            verify(productRepository, times(1)).save(product);
            assertThat(result).isEqualTo(product);
        }

        @DisplayName("좋아요가 없으면, delete/decreaseLikes/productRepository.save를 호출하지 않고 product를 반환한다 (멱등).")
        @Test
        void doesNotDeleteOrDecrease_whenLikeDoesNotExist() {
            // arrange
            Product product = mock(Product.class);
            when(productRepository.findByIdWithLock(PRODUCT_ID)).thenReturn(Optional.of(product));
            when(likeRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(Optional.empty());

            // act
            Product result = likeApplicationService.unlike(USER_ID, PRODUCT_ID);

            // assert
            verify(likeRepository, never()).delete(any());
            verify(product, never()).decreaseLikes();
            verify(productRepository, never()).save(any());
            assertThat(result).isEqualTo(product);
        }

        @DisplayName("상품이 존재하지 않으면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFound_whenProductDoesNotExist() {
            // arrange
            when(productRepository.findByIdWithLock(PRODUCT_ID)).thenReturn(Optional.empty());

            // act
            CoreException ex = assertThrows(CoreException.class, () -> likeApplicationService.unlike(USER_ID, PRODUCT_ID));

            // assert
            assertThat(ex.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
            verify(likeRepository, never()).delete(any());
        }
    }
}
