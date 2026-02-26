package com.loopers.interfaces.api;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.SellingStatus;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.interfaces.api.product.ProductAdminV1Dto;
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
class ProductAdminV1ApiE2ETest {

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

    private Brand createBrand(String name) {
        return brandJpaRepository.save(new Brand(name, null));
    }

    private Product createProduct(Long brandId, String name, int price) {
        return productJpaRepository.save(new Product(brandId, name, null, price, 10, SellingStatus.SELLING));
    }

    private HttpHeaders createAdminHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(ADMIN_HEADER, ADMIN_VALUE);
        return headers;
    }

    @DisplayName("GET /api-admin/v1/products")
    @Nested
    class GetProducts {

        @DisplayName("Admin 헤더 없이 요청하면, 401 Unauthorized를 반환한다.")
        @Test
        void returns401_whenAdminHeaderMissing() {
            // arrange & act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api-admin/v1/products",
                HttpMethod.GET,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @DisplayName("Admin 헤더로 요청하면, 200 OK와 페이징된 상품 목록을 반환한다.")
        @Test
        void returns200WithPagedProducts_whenAdminHeaderPresent() {
            // arrange
            Brand brand = createBrand("Nike");
            createProduct(brand.getId(), "에어맥스", 100000);
            createProduct(brand.getId(), "조던", 200000);

            // act
            ResponseEntity<ApiResponse<ProductAdminV1Dto.ProductPageResponse>> response = testRestTemplate.exchange(
                "/api-admin/v1/products?page=0&size=10",
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

        @DisplayName("brandId 필터를 적용하면, 해당 브랜드의 상품만 반환한다.")
        @Test
        void returns200WithBrandIdFilter_whenAdminHeaderPresent() {
            // arrange
            Brand nike = createBrand("Nike");
            Brand adidas = createBrand("Adidas");
            createProduct(nike.getId(), "에어맥스", 100000);
            createProduct(adidas.getId(), "슈퍼스타", 80000);

            // act
            ResponseEntity<ApiResponse<ProductAdminV1Dto.ProductPageResponse>> response = testRestTemplate.exchange(
                "/api-admin/v1/products?brandId=" + nike.getId() + "&page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(createAdminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().content()).hasSize(1),
                () -> assertThat(response.getBody().data().content().get(0).brandId()).isEqualTo(nike.getId())
            );
        }
    }

    @DisplayName("GET /api-admin/v1/products/{productId}")
    @Nested
    class GetProduct {

        @DisplayName("Admin 헤더 없이 요청하면, 401 Unauthorized를 반환한다.")
        @Test
        void returns401_whenAdminHeaderMissing() {
            // arrange
            Brand brand = createBrand("Nike");
            Product product = createProduct(brand.getId(), "에어맥스", 100000);

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api-admin/v1/products/" + product.getId(),
                HttpMethod.GET,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @DisplayName("존재하는 productId로 조회하면, 200 OK와 상품 정보를 반환한다.")
        @Test
        void returns200_whenProductExists() {
            // arrange
            Brand brand = createBrand("Nike");
            Product product = createProduct(brand.getId(), "에어맥스", 100000);

            // act
            ResponseEntity<ApiResponse<ProductAdminV1Dto.ProductResponse>> response = testRestTemplate.exchange(
                "/api-admin/v1/products/" + product.getId(),
                HttpMethod.GET,
                new HttpEntity<>(createAdminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data().id()).isEqualTo(product.getId()),
                () -> assertThat(response.getBody().data().name()).isEqualTo("에어맥스"),
                () -> assertThat(response.getBody().data().price()).isEqualTo(100000)
            );
        }

        @DisplayName("존재하지 않는 productId로 조회하면, 404 Not Found를 반환한다.")
        @Test
        void returns404_whenProductNotFound() {
            // arrange & act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api-admin/v1/products/99999",
                HttpMethod.GET,
                new HttpEntity<>(createAdminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @DisplayName("POST /api-admin/v1/products")
    @Nested
    class CreateProduct {

        @DisplayName("Admin 헤더 없이 요청하면, 401 Unauthorized를 반환한다.")
        @Test
        void returns401_whenAdminHeaderMissing() {
            // arrange
            Brand brand = createBrand("Nike");
            ProductAdminV1Dto.CreateRequest request = new ProductAdminV1Dto.CreateRequest(
                brand.getId(), "에어맥스", null, 100000, 10, SellingStatus.SELLING
            );

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api-admin/v1/products",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @DisplayName("정상적인 요청으로 상품을 등록하면, 201 Created와 생성된 상품 정보를 반환한다.")
        @Test
        void returns201_whenProductCreatedSuccessfully() {
            // arrange
            Brand brand = createBrand("Nike");
            ProductAdminV1Dto.CreateRequest request = new ProductAdminV1Dto.CreateRequest(
                brand.getId(), "에어맥스", "나이키 에어맥스", 100000, 10, SellingStatus.SELLING
            );

            // act
            ResponseEntity<ApiResponse<ProductAdminV1Dto.ProductResponse>> response = testRestTemplate.exchange(
                "/api-admin/v1/products",
                HttpMethod.POST,
                new HttpEntity<>(request, createAdminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data().id()).isNotNull(),
                () -> assertThat(response.getBody().data().name()).isEqualTo("에어맥스"),
                () -> assertThat(response.getBody().data().brandId()).isEqualTo(brand.getId()),
                () -> assertThat(response.getBody().data().price()).isEqualTo(100000)
            );
        }

        @DisplayName("존재하지 않는 brandId로 등록하면, 404 Not Found를 반환한다.")
        @Test
        void returns404_whenBrandNotFound() {
            // arrange
            ProductAdminV1Dto.CreateRequest request = new ProductAdminV1Dto.CreateRequest(
                99999L, "에어맥스", null, 100000, 10, SellingStatus.SELLING
            );

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api-admin/v1/products",
                HttpMethod.POST,
                new HttpEntity<>(request, createAdminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @DisplayName("name이 blank이면, 400 Bad Request를 반환한다.")
        @Test
        void returns400_whenNameIsBlank() {
            // arrange
            Brand brand = createBrand("Nike");
            ProductAdminV1Dto.CreateRequest request = new ProductAdminV1Dto.CreateRequest(
                brand.getId(), "", null, 100000, 10, SellingStatus.SELLING
            );

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api-admin/v1/products",
                HttpMethod.POST,
                new HttpEntity<>(request, createAdminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @DisplayName("PATCH /api-admin/v1/products/{productId}")
    @Nested
    class UpdateProduct {

        @DisplayName("Admin 헤더 없이 요청하면, 401 Unauthorized를 반환한다.")
        @Test
        void returns401_whenAdminHeaderMissing() {
            // arrange
            Brand brand = createBrand("Nike");
            Product product = createProduct(brand.getId(), "에어맥스", 100000);
            ProductAdminV1Dto.UpdateRequest request = new ProductAdminV1Dto.UpdateRequest(
                "에어맥스 V2", null, 120000, SellingStatus.SELLING
            );

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api-admin/v1/products/" + product.getId(),
                HttpMethod.PATCH,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @DisplayName("정상적인 요청으로 상품을 수정하면, 200 OK와 수정된 상품 정보를 반환한다.")
        @Test
        void returns200_whenProductUpdatedSuccessfully() {
            // arrange
            Brand brand = createBrand("Nike");
            Product product = createProduct(brand.getId(), "에어맥스", 100000);
            ProductAdminV1Dto.UpdateRequest request = new ProductAdminV1Dto.UpdateRequest(
                "에어맥스 V2", "업데이트된 에어맥스", 120000, SellingStatus.SELLING
            );

            // act
            ResponseEntity<ApiResponse<ProductAdminV1Dto.ProductResponse>> response = testRestTemplate.exchange(
                "/api-admin/v1/products/" + product.getId(),
                HttpMethod.PATCH,
                new HttpEntity<>(request, createAdminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data().name()).isEqualTo("에어맥스 V2"),
                () -> assertThat(response.getBody().data().description()).isEqualTo("업데이트된 에어맥스"),
                () -> assertThat(response.getBody().data().price()).isEqualTo(120000)
            );
        }

        @DisplayName("존재하지 않는 productId로 수정하면, 404 Not Found를 반환한다.")
        @Test
        void returns404_whenProductNotFound() {
            // arrange
            ProductAdminV1Dto.UpdateRequest request = new ProductAdminV1Dto.UpdateRequest(
                "에어맥스 V2", null, 120000, SellingStatus.SELLING
            );

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api-admin/v1/products/99999",
                HttpMethod.PATCH,
                new HttpEntity<>(request, createAdminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @DisplayName("DELETE /api-admin/v1/products/{productId}")
    @Nested
    class DeleteProduct {

        @DisplayName("Admin 헤더 없이 요청하면, 401 Unauthorized를 반환한다.")
        @Test
        void returns401_whenAdminHeaderMissing() {
            // arrange
            Brand brand = createBrand("Nike");
            Product product = createProduct(brand.getId(), "에어맥스", 100000);

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api-admin/v1/products/" + product.getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @DisplayName("정상적으로 상품을 삭제하면, 204 No Content를 반환한다.")
        @Test
        void returns204_whenProductDeletedSuccessfully() {
            // arrange
            Brand brand = createBrand("Nike");
            Product product = createProduct(brand.getId(), "에어맥스", 100000);

            // act
            ResponseEntity<Void> response = testRestTemplate.exchange(
                "/api-admin/v1/products/" + product.getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(createAdminHeaders()),
                Void.class
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        @DisplayName("존재하지 않는 productId로 삭제하면, 404 Not Found를 반환한다.")
        @Test
        void returns404_whenProductNotFound() {
            // arrange & act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api-admin/v1/products/99999",
                HttpMethod.DELETE,
                new HttpEntity<>(createAdminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
