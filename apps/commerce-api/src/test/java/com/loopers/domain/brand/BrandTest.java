package com.loopers.domain.brand;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class BrandTest {

    private static final String NAME = "나이키";
    private static final String DESCRIPTION = "Just Do It";

    @DisplayName("브랜드를 생성할 때,")
    @Nested
    class Create {

        @DisplayName("이름과 설명이 주어지면, 정상적으로 생성된다.")
        @Test
        void createsBrand_whenNameAndDescriptionAreProvided() {
            // arrange & act
            Brand brand = new Brand(NAME, DESCRIPTION);

            // assert
            assertAll(
                () -> assertThat(brand.getName()).isEqualTo(NAME),
                () -> assertThat(brand.getDescription()).isEqualTo(DESCRIPTION)
            );
        }

        @DisplayName("이름이 빈값이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenNameIsBlank() {
            // arrange & act & assert
            assertThatThrownBy(() -> new Brand("", DESCRIPTION))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @DisplayName("설명이 null이면, 정상적으로 생성된다.")
        @Test
        void createsBrand_whenDescriptionIsNull() {
            // arrange & act
            Brand brand = new Brand(NAME, null);

            // assert
            assertThat(brand.getDescription()).isNull();
        }
    }

    @DisplayName("브랜드 정보를 변경할 때,")
    @Nested
    class ChangeBrandInfo {

        @DisplayName("유효한 값으로 변경하면, 필드가 업데이트된다.")
        @Test
        void updatesBrandInfo_whenValidValuesAreProvided() {
            // arrange
            Brand brand = new Brand(NAME, DESCRIPTION);
            String newName = "아디다스";
            String newDescription = "Impossible is Nothing";

            // act
            brand.changeBrandInfo(newName, newDescription);

            // assert
            assertAll(
                () -> assertThat(brand.getName()).isEqualTo(newName),
                () -> assertThat(brand.getDescription()).isEqualTo(newDescription)
            );
        }

        @DisplayName("변경할 이름이 빈값이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenNewNameIsBlank() {
            // arrange
            Brand brand = new Brand(NAME, DESCRIPTION);

            // act & assert
            assertThatThrownBy(() -> brand.changeBrandInfo("", DESCRIPTION))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }
    }
}
