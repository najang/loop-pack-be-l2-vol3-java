package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final Long BRAND_ID = 1L;
    private static final String NAME = "에어맥스";
    private static final String DESCRIPTION = "Nike Air Max";
    private static final int PRICE = 100000;
    private static final int STOCK = 10;
    private static final SellingStatus SELLING_STATUS = SellingStatus.SELLING;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @DisplayName("상품 조회 시,")
    @Nested
    class FindById {

        @DisplayName("존재하는 ID면, 상품을 반환한다.")
        @Test
        void returnsProduct_whenProductExists() {
            // arrange
            Product product = mock(Product.class);
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            // act
            Product found = productService.findById(1L);

            // assert
            assertThat(found).isSameAs(product);
        }

        @DisplayName("존재하지 않는 ID면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFound_whenProductDoesNotExist() {
            // arrange
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            // act
            CoreException ex = assertThrows(CoreException.class, () -> productService.findById(999L));

            // assert
            assertThat(ex.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }

    @DisplayName("상품 생성 시,")
    @Nested
    class Create {

        @DisplayName("저장된 Product를 반환한다.")
        @Test
        void returnsSavedProduct_whenCreated() {
            // arrange
            when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

            // act
            Product created = productService.create(BRAND_ID, NAME, DESCRIPTION, PRICE, STOCK, SELLING_STATUS);

            // assert
            assertThat(created).isNotNull();
            assertThat(created.getName()).isEqualTo(NAME);
            assertThat(created.getBrandId()).isEqualTo(BRAND_ID);
        }
    }

    @DisplayName("상품 수정 시,")
    @Nested
    class Update {

        @DisplayName("changeProductInfo 호출 후 save한다.")
        @Test
        void callsChangeProductInfoAndSave() {
            // arrange
            Product product = mock(Product.class);
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(productRepository.save(product)).thenReturn(product);

            // act
            productService.update(1L, "에어조던", "Nike Air Jordan", 200000, SellingStatus.STOP);

            // assert
            verify(product, times(1)).changeProductInfo("에어조던", "Nike Air Jordan", 200000, SellingStatus.STOP);
            verify(productRepository, times(1)).save(product);
        }
    }

    @DisplayName("상품 삭제 시,")
    @Nested
    class Delete {

        @DisplayName("product.delete() 호출 후 save한다.")
        @Test
        void callsDeleteAndSave() {
            // arrange
            Product product = mock(Product.class);
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(productRepository.save(product)).thenReturn(product);

            // act
            productService.delete(1L);

            // assert
            verify(product, times(1)).delete();
            verify(productRepository, times(1)).save(product);
        }
    }

    @DisplayName("상품 전체 조회 시,")
    @Nested
    class FindAll {

        @DisplayName("brandId 없이 조회하면, 전체 상품 페이지를 반환한다.")
        @Test
        void returnsAllProducts_whenBrandIdIsNull() {
            // arrange
            PageRequest pageable = PageRequest.of(0, 10);
            Page<Product> page = new PageImpl<>(List.of(mock(Product.class)));
            when(productRepository.findAll(isNull(), any())).thenReturn(page);

            // act
            Page<Product> result = productService.findAll(null, pageable);

            // assert
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @DisplayName("brandId를 지정하면, 해당 브랜드 상품 페이지를 반환한다.")
        @Test
        void returnsFilteredProducts_whenBrandIdIsProvided() {
            // arrange
            PageRequest pageable = PageRequest.of(0, 10);
            Page<Product> page = new PageImpl<>(List.of(mock(Product.class), mock(Product.class)));
            when(productRepository.findAll(BRAND_ID, pageable)).thenReturn(page);

            // act
            Page<Product> result = productService.findAll(BRAND_ID, pageable);

            // assert
            assertThat(result.getTotalElements()).isEqualTo(2);
        }
    }
}
