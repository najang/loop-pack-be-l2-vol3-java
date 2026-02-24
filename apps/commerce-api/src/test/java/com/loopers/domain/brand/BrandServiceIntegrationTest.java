package com.loopers.domain.brand;

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
class BrandServiceIntegrationTest {

    private static final String NAME = "나이키";
    private static final String DESCRIPTION = "Just Do It";

    @Autowired
    private BrandService brandService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("브랜드 생성 후 조회 시,")
    @Nested
    class CreateAndFind {

        @DisplayName("create 후 findById로 조회 성공한다.")
        @Test
        void findsBrand_afterCreate() {
            // arrange
            Brand created = brandService.create(NAME, DESCRIPTION);

            // act
            Brand found = brandService.findById(created.getId());

            // assert
            assertAll(
                () -> assertThat(found.getId()).isEqualTo(created.getId()),
                () -> assertThat(found.getName()).isEqualTo(NAME),
                () -> assertThat(found.getDescription()).isEqualTo(DESCRIPTION)
            );
        }
    }

    @DisplayName("브랜드 삭제 시,")
    @Nested
    class Delete {

        @DisplayName("delete 후 findById → NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFound_afterDelete() {
            // arrange
            Brand created = brandService.create(NAME, DESCRIPTION);
            brandService.delete(created.getId());

            // act
            CoreException ex = assertThrows(CoreException.class, () -> brandService.findById(created.getId()));

            // assert
            assertThat(ex.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }

    @DisplayName("브랜드 수정 시,")
    @Nested
    class Update {

        @DisplayName("update 후 findById → 변경된 값을 확인한다.")
        @Test
        void returnsUpdatedBrand_afterUpdate() {
            // arrange
            Brand created = brandService.create(NAME, DESCRIPTION);
            String newName = "아디다스";
            String newDescription = "Impossible is Nothing";

            // act
            brandService.update(created.getId(), newName, newDescription);
            Brand updated = brandService.findById(created.getId());

            // assert
            assertAll(
                () -> assertThat(updated.getName()).isEqualTo(newName),
                () -> assertThat(updated.getDescription()).isEqualTo(newDescription)
            );
        }
    }

    @DisplayName("브랜드 전체 조회 시,")
    @Nested
    class FindAll {

        @DisplayName("삭제된 브랜드는 제외하고 반환한다.")
        @Test
        void excludesDeletedBrands_whenFindAll() {
            // arrange
            Brand active = brandService.create(NAME, DESCRIPTION);
            Brand toDelete = brandService.create("리복", "Forever");
            brandService.delete(toDelete.getId());

            // act
            Page<Brand> result = brandService.findAll(PageRequest.of(0, 10));

            // assert
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(active.getId());
        }
    }
}
