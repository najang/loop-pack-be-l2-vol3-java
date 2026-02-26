package com.loopers.interfaces.api;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.SellingStatus;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.interfaces.api.brand.BrandAdminV1Dto;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BrandAdminV1ApiE2ETest {

    private static final String ADMIN_HEADER = "X-Loopers-Ldap";
    private static final String ADMIN_VALUE = "loopers.admin";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private BrandJpaRepository brandJpaRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private Brand createBrand(String name, String description) {
        return brandJpaRepository.save(new Brand(name, description));
    }

    private HttpHeaders createAdminHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(ADMIN_HEADER, ADMIN_VALUE);
        return headers;
    }

    @DisplayName("GET /api-admin/v1/brands")
    @Nested
    class GetBrands {

        @DisplayName("Admin 헤더 없이 요청하면, 401 Unauthorized를 반환한다.")
        @Test
        void returns401_whenAdminHeaderMissing() {
            // arrange & act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api-admin/v1/brands",
                HttpMethod.GET,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @DisplayName("Admin 헤더로 요청하면, 200 OK와 페이징된 브랜드 목록을 반환한다.")
        @Test
        void returns200WithPagedBrands_whenAdminHeaderPresent() {
            // arrange
            createBrand("Nike", "나이키");
            createBrand("Adidas", "아디다스");

            // act
            ResponseEntity<ApiResponse<BrandAdminV1Dto.BrandPageResponse>> response = testRestTemplate.exchange(
                "/api-admin/v1/brands?page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(createAdminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data().content()).hasSize(2),
                () -> assertThat(response.getBody().data().totalElements()).isEqualTo(2)
            );
        }
    }

    @DisplayName("GET /api-admin/v1/brands/{brandId}")
    @Nested
    class GetBrand {

        @DisplayName("Admin 헤더 없이 요청하면, 401 Unauthorized를 반환한다.")
        @Test
        void returns401_whenAdminHeaderMissing() {
            // arrange
            Brand brand = createBrand("Nike", "나이키");

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api-admin/v1/brands/" + brand.getId(),
                HttpMethod.GET,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @DisplayName("존재하는 brandId로 조회하면, 200 OK와 브랜드 정보를 반환한다.")
        @Test
        void returns200_whenBrandExists() {
            // arrange
            Brand brand = createBrand("Nike", "나이키 브랜드");

            // act
            ResponseEntity<ApiResponse<BrandAdminV1Dto.BrandResponse>> response = testRestTemplate.exchange(
                "/api-admin/v1/brands/" + brand.getId(),
                HttpMethod.GET,
                new HttpEntity<>(createAdminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data().id()).isEqualTo(brand.getId()),
                () -> assertThat(response.getBody().data().name()).isEqualTo("Nike")
            );
        }

        @DisplayName("존재하지 않는 brandId로 조회하면, 404 Not Found를 반환한다.")
        @Test
        void returns404_whenBrandNotFound() {
            // arrange & act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api-admin/v1/brands/99999",
                HttpMethod.GET,
                new HttpEntity<>(createAdminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @DisplayName("POST /api-admin/v1/brands")
    @Nested
    class CreateBrand {

        @DisplayName("Admin 헤더 없이 요청하면, 401 Unauthorized를 반환한다.")
        @Test
        void returns401_whenAdminHeaderMissing() {
            // arrange
            BrandAdminV1Dto.CreateRequest request = new BrandAdminV1Dto.CreateRequest("Nike", "나이키");

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api-admin/v1/brands",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @DisplayName("정상적인 요청으로 브랜드를 등록하면, 201 Created와 생성된 브랜드 정보를 반환한다.")
        @Test
        void returns201_whenBrandCreatedSuccessfully() {
            // arrange
            BrandAdminV1Dto.CreateRequest request = new BrandAdminV1Dto.CreateRequest("Nike", "나이키 브랜드");

            // act
            ResponseEntity<ApiResponse<BrandAdminV1Dto.BrandResponse>> response = testRestTemplate.exchange(
                "/api-admin/v1/brands",
                HttpMethod.POST,
                new HttpEntity<>(request, createAdminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data().id()).isNotNull(),
                () -> assertThat(response.getBody().data().name()).isEqualTo("Nike"),
                () -> assertThat(response.getBody().data().description()).isEqualTo("나이키 브랜드")
            );
        }

        @DisplayName("name이 blank이면, 400 Bad Request를 반환한다.")
        @Test
        void returns400_whenNameIsBlank() {
            // arrange
            BrandAdminV1Dto.CreateRequest request = new BrandAdminV1Dto.CreateRequest("", "나이키 브랜드");

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api-admin/v1/brands",
                HttpMethod.POST,
                new HttpEntity<>(request, createAdminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @DisplayName("PATCH /api-admin/v1/brands/{brandId}")
    @Nested
    class UpdateBrand {

        @DisplayName("Admin 헤더 없이 요청하면, 401 Unauthorized를 반환한다.")
        @Test
        void returns401_whenAdminHeaderMissing() {
            // arrange
            Brand brand = createBrand("Nike", "나이키");
            BrandAdminV1Dto.UpdateRequest request = new BrandAdminV1Dto.UpdateRequest("Nike Updated", "업데이트");

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api-admin/v1/brands/" + brand.getId(),
                HttpMethod.PATCH,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @DisplayName("정상적인 요청으로 브랜드를 수정하면, 200 OK와 수정된 브랜드 정보를 반환한다.")
        @Test
        void returns200_whenBrandUpdatedSuccessfully() {
            // arrange
            Brand brand = createBrand("Nike", "나이키");
            BrandAdminV1Dto.UpdateRequest request = new BrandAdminV1Dto.UpdateRequest("Nike Updated", "업데이트된 나이키");

            // act
            ResponseEntity<ApiResponse<BrandAdminV1Dto.BrandResponse>> response = testRestTemplate.exchange(
                "/api-admin/v1/brands/" + brand.getId(),
                HttpMethod.PATCH,
                new HttpEntity<>(request, createAdminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data().name()).isEqualTo("Nike Updated"),
                () -> assertThat(response.getBody().data().description()).isEqualTo("업데이트된 나이키")
            );
        }

        @DisplayName("존재하지 않는 brandId로 수정하면, 404 Not Found를 반환한다.")
        @Test
        void returns404_whenBrandNotFound() {
            // arrange
            BrandAdminV1Dto.UpdateRequest request = new BrandAdminV1Dto.UpdateRequest("Nike Updated", "업데이트");

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api-admin/v1/brands/99999",
                HttpMethod.PATCH,
                new HttpEntity<>(request, createAdminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @DisplayName("DELETE /api-admin/v1/brands/{brandId}")
    @Nested
    class DeleteBrand {

        @DisplayName("Admin 헤더 없이 요청하면, 401 Unauthorized를 반환한다.")
        @Test
        void returns401_whenAdminHeaderMissing() {
            // arrange
            Brand brand = createBrand("Nike", "나이키");

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api-admin/v1/brands/" + brand.getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @DisplayName("정상적으로 브랜드를 삭제하면, 204 No Content를 반환한다.")
        @Test
        void returns204_whenBrandDeletedSuccessfully() {
            // arrange
            Brand brand = createBrand("Nike", "나이키");

            // act
            ResponseEntity<Void> response = testRestTemplate.exchange(
                "/api-admin/v1/brands/" + brand.getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(createAdminHeaders()),
                Void.class
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        @DisplayName("브랜드 삭제 시 연관된 상품도 함께 삭제된다.")
        @Test
        void deletesAssociatedProducts_whenBrandDeleted() {
            // arrange
            Brand brand = createBrand("Nike", "나이키");
            com.loopers.domain.product.Product product = new com.loopers.domain.product.Product(
                brand.getId(), "에어맥스", "나이키 에어맥스", 150000, 10, SellingStatus.SELLING
            );
            productJpaRepository.save(product);

            // act
            testRestTemplate.exchange(
                "/api-admin/v1/brands/" + brand.getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(createAdminHeaders()),
                Void.class
            );

            // assert
            long count = productJpaRepository.findAll().stream()
                .filter(p -> p.getBrandId().equals(brand.getId()) && p.getDeletedAt() == null)
                .count();
            assertThat(count).isZero();
        }

        @DisplayName("존재하지 않는 brandId로 삭제하면, 404 Not Found를 반환한다.")
        @Test
        void returns404_whenBrandNotFound() {
            // arrange & act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api-admin/v1/brands/99999",
                HttpMethod.DELETE,
                new HttpEntity<>(createAdminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
