package com.loopers.interfaces.api;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.SellingStatus;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.interfaces.api.product.ProductV1Dto;
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
class ProductV1ApiE2ETest {

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

    private Product createProduct(Long brandId, String name, int price, int likeCount) {
        Product product = new Product(brandId, name, null, price, 10, SellingStatus.SELLING);
        for (int i = 0; i < likeCount; i++) {
            product.increaseLikes();
        }
        return productJpaRepository.save(product);
    }

    @DisplayName("GET /api/v1/products")
    @Nested
    class GetProducts {

        @DisplayName("인증 없이 요청해도 200 OK와 페이징된 상품 목록을 반환한다.")
        @Test
        void returns200WithPagedProducts_withoutAuth() {
            // arrange
            Brand brand = createBrand("Nike");
            createProduct(brand.getId(), "에어맥스", 100000, 0);
            createProduct(brand.getId(), "조던", 200000, 0);

            // act
            ResponseEntity<ApiResponse<ProductV1Dto.ProductPageResponse>> response = testRestTemplate.exchange(
                "/api/v1/products?page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(null),
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

        @DisplayName("sort=latest 파라미터로 조회하면, 최신 등록 순으로 정렬된다.")
        @Test
        void returns200WithLatestSort() {
            // arrange
            Brand brand = createBrand("Nike");
            Product older = createProduct(brand.getId(), "에어맥스", 100000, 0);
            Product newer = createProduct(brand.getId(), "조던", 200000, 0);

            // act
            ResponseEntity<ApiResponse<ProductV1Dto.ProductPageResponse>> response = testRestTemplate.exchange(
                "/api/v1/products?sort=latest&page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().content()).hasSize(2),
                () -> assertThat(response.getBody().data().content().get(0).id()).isEqualTo(newer.getId()),
                () -> assertThat(response.getBody().data().content().get(1).id()).isEqualTo(older.getId())
            );
        }

        @DisplayName("sort=price_asc 파라미터로 조회하면, 가격 오름차순으로 정렬된다.")
        @Test
        void returns200WithPriceAscSort() {
            // arrange
            Brand brand = createBrand("Nike");
            Product expensive = createProduct(brand.getId(), "조던", 200000, 0);
            Product cheap = createProduct(brand.getId(), "에어맥스", 50000, 0);

            // act
            ResponseEntity<ApiResponse<ProductV1Dto.ProductPageResponse>> response = testRestTemplate.exchange(
                "/api/v1/products?sort=price_asc&page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().content()).hasSize(2),
                () -> assertThat(response.getBody().data().content().get(0).id()).isEqualTo(cheap.getId()),
                () -> assertThat(response.getBody().data().content().get(1).id()).isEqualTo(expensive.getId())
            );
        }

        @DisplayName("sort=likes_desc 파라미터로 조회하면, 좋아요 내림차순으로 정렬된다.")
        @Test
        void returns200WithLikesDescSort() {
            // arrange
            Brand brand = createBrand("Nike");
            Product fewLikes = createProduct(brand.getId(), "에어맥스", 100000, 1);
            Product manyLikes = createProduct(brand.getId(), "조던", 200000, 5);

            // act
            ResponseEntity<ApiResponse<ProductV1Dto.ProductPageResponse>> response = testRestTemplate.exchange(
                "/api/v1/products?sort=likes_desc&page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().content()).hasSize(2),
                () -> assertThat(response.getBody().data().content().get(0).id()).isEqualTo(manyLikes.getId()),
                () -> assertThat(response.getBody().data().content().get(1).id()).isEqualTo(fewLikes.getId())
            );
        }

        @DisplayName("brandId 필터를 적용하면, 해당 브랜드의 상품만 반환한다.")
        @Test
        void returns200WithBrandIdFilter() {
            // arrange
            Brand nike = createBrand("Nike");
            Brand adidas = createBrand("Adidas");
            createProduct(nike.getId(), "에어맥스", 100000, 0);
            createProduct(adidas.getId(), "슈퍼스타", 80000, 0);

            // act
            ResponseEntity<ApiResponse<ProductV1Dto.ProductPageResponse>> response = testRestTemplate.exchange(
                "/api/v1/products?brandId=" + nike.getId() + "&page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(null),
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

    @DisplayName("GET /api/v1/products/{productId}")
    @Nested
    class GetProduct {

        @DisplayName("존재하는 productId로 조회하면, 200 OK와 상품 정보를 반환한다.")
        @Test
        void returns200_whenProductExists() {
            // arrange
            Brand brand = createBrand("Nike");
            Product product = createProduct(brand.getId(), "에어맥스", 100000, 0);

            // act
            ResponseEntity<ApiResponse<ProductV1Dto.ProductResponse>> response = testRestTemplate.exchange(
                "/api/v1/products/" + product.getId(),
                HttpMethod.GET,
                new HttpEntity<>(null),
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
                "/api/v1/products/99999",
                HttpMethod.GET,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
