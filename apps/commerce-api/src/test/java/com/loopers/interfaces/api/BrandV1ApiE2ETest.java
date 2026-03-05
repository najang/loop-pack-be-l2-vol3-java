package com.loopers.interfaces.api;

import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.domain.brand.Brand;
import com.loopers.interfaces.api.brand.BrandV1Dto;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BrandV1ApiE2ETest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private BrandJpaRepository brandJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private Brand createBrand(String name, String description) {
        return brandJpaRepository.save(new Brand(name, description));
    }

    @DisplayName("GET /api/v1/brands/{brandId}")
    @Nested
    class GetBrand {

        @DisplayName("존재하는 brandId로 조회하면, 200 OK와 브랜드 정보를 반환한다.")
        @Test
        void returns200_whenBrandExists() {
            // arrange
            Brand brand = createBrand("Nike", "나이키 브랜드");

            // act
            ResponseEntity<ApiResponse<BrandV1Dto.BrandResponse>> response = testRestTemplate.exchange(
                "/api/v1/brands/" + brand.getId(),
                HttpMethod.GET,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data().id()).isEqualTo(brand.getId()),
                () -> assertThat(response.getBody().data().name()).isEqualTo("Nike"),
                () -> assertThat(response.getBody().data().description()).isEqualTo("나이키 브랜드")
            );
        }

        @DisplayName("존재하지 않는 brandId로 조회하면, 404 Not Found를 반환한다.")
        @Test
        void returns404_whenBrandNotFound() {
            // arrange & act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/brands/99999",
                HttpMethod.GET,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @DisplayName("인증 헤더 없이도 조회할 수 있다.")
        @Test
        void returns200_withoutAuthHeader() {
            // arrange
            Brand brand = createBrand("Adidas", "아디다스 브랜드");

            // act
            ResponseEntity<ApiResponse<BrandV1Dto.BrandResponse>> response = testRestTemplate.exchange(
                "/api/v1/brands/" + brand.getId(),
                HttpMethod.GET,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }
}
