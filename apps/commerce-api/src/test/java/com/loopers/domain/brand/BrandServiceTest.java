package com.loopers.domain.brand;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    private static final String NAME = "나이키";
    private static final String DESCRIPTION = "Just Do It";

    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private BrandService brandService;

    @DisplayName("브랜드 조회 시,")
    @Nested
    class FindById {

        @DisplayName("존재하는 ID면, 브랜드를 반환한다.")
        @Test
        void returnsBrand_whenBrandExists() {
            // arrange
            Brand brand = mock(Brand.class);
            when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

            // act
            Brand found = brandService.findById(1L);

            // assert
            assertThat(found).isSameAs(brand);
        }

        @DisplayName("존재하지 않는 ID면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFound_whenBrandDoesNotExist() {
            // arrange
            when(brandRepository.findById(999L)).thenReturn(Optional.empty());

            // act
            CoreException ex = assertThrows(CoreException.class, () -> brandService.findById(999L));

            // assert
            assertThat(ex.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }

    @DisplayName("브랜드 생성 시,")
    @Nested
    class Create {

        @DisplayName("저장된 Brand를 반환한다.")
        @Test
        void returnsSavedBrand_whenCreated() {
            // arrange
            when(brandRepository.save(any(Brand.class))).thenAnswer(inv -> inv.getArgument(0));

            // act
            Brand created = brandService.create(NAME, DESCRIPTION);

            // assert
            assertThat(created).isNotNull();
            assertThat(created.getName()).isEqualTo(NAME);
            assertThat(created.getDescription()).isEqualTo(DESCRIPTION);
        }
    }

    @DisplayName("브랜드 수정 시,")
    @Nested
    class Update {

        @DisplayName("changeBrandInfo 호출 후 save한다.")
        @Test
        void callsChangeBrandInfoAndSave() {
            // arrange
            Brand brand = mock(Brand.class);
            when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
            when(brandRepository.save(brand)).thenReturn(brand);

            // act
            brandService.update(1L, "아디다스", "Impossible is Nothing");

            // assert
            verify(brand, times(1)).changeBrandInfo("아디다스", "Impossible is Nothing");
            verify(brandRepository, times(1)).save(brand);
        }
    }

    @DisplayName("브랜드 삭제 시,")
    @Nested
    class Delete {

        @DisplayName("brand.delete() 호출 후 save한다.")
        @Test
        void callsDeleteAndSave() {
            // arrange
            Brand brand = mock(Brand.class);
            when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
            when(brandRepository.save(brand)).thenReturn(brand);

            // act
            brandService.delete(1L);

            // assert
            verify(brand, times(1)).delete();
            verify(brandRepository, times(1)).save(brand);
        }
    }

    @DisplayName("브랜드 전체 조회 시,")
    @Nested
    class FindAll {

        @DisplayName("페이지 결과를 반환한다.")
        @Test
        void returnsPagedBrands() {
            // arrange
            PageRequest pageable = PageRequest.of(0, 10);
            Page<Brand> page = new PageImpl<>(List.of(mock(Brand.class)));
            when(brandRepository.findAll(pageable)).thenReturn(page);

            // act
            Page<Brand> result = brandService.findAll(pageable);

            // assert
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }
}
