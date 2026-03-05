package com.loopers.interfaces.api;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.SellingStatus;
import com.loopers.domain.user.PasswordEncoder;
import com.loopers.domain.user.UserModel;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.interfaces.api.like.LikeV1Dto;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LikeV1ApiE2ETest {

    private static final String LOGIN_ID = "testuser1";
    private static final String OTHER_LOGIN_ID = "otheruser1";
    private static final String RAW_PASSWORD = "Test1234!";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    private UserModel createUser(String loginId) {
        return userJpaRepository.save(new UserModel(
            loginId,
            passwordEncoder.encode(RAW_PASSWORD),
            "홍길동",
            LocalDate.of(1990, 1, 15),
            loginId + "@example.com"
        ));
    }

    private HttpHeaders createAuthHeaders(String loginId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Loopers-LoginId", loginId);
        headers.set("X-Loopers-LoginPw", RAW_PASSWORD);
        return headers;
    }

    private Product createProduct(String name) {
        Brand brand = brandJpaRepository.save(new Brand("Nike", null));
        return productJpaRepository.save(new Product(brand.getId(), name, null, 100000, 10, SellingStatus.SELLING));
    }

    @DisplayName("POST /api/v1/products/{productId}/likes")
    @Nested
    class LikeProduct {

        @DisplayName("인증 없이 요청하면, 401 Unauthorized를 반환한다.")
        @Test
        void returns401_whenNoAuth() {
            // arrange
            Product product = createProduct("에어맥스");

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/products/" + product.getId() + "/likes",
                HttpMethod.POST,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @DisplayName("정상 좋아요 추가 시 200 OK와 liked=true, likeCount=1을 반환한다.")
        @Test
        void returns200WithLikedTrue_whenLikeAdded() {
            // arrange
            createUser(LOGIN_ID);
            Product product = createProduct("에어맥스");

            // act
            ResponseEntity<ApiResponse<LikeV1Dto.LikeResponse>> response = testRestTemplate.exchange(
                "/api/v1/products/" + product.getId() + "/likes",
                HttpMethod.POST,
                new HttpEntity<>(createAuthHeaders(LOGIN_ID)),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data().liked()).isTrue(),
                () -> assertThat(response.getBody().data().likeCount()).isEqualTo(1)
            );
        }

        @DisplayName("같은 상품에 좋아요를 다시 요청해도 200 OK이고 likeCount가 1로 유지된다 (멱등).")
        @Test
        void returns200AndLikeCountRemainsOne_whenLikedAgain() {
            // arrange
            createUser(LOGIN_ID);
            Product product = createProduct("에어맥스");
            testRestTemplate.exchange(
                "/api/v1/products/" + product.getId() + "/likes",
                HttpMethod.POST,
                new HttpEntity<>(createAuthHeaders(LOGIN_ID)),
                new ParameterizedTypeReference<ApiResponse<LikeV1Dto.LikeResponse>>() {}
            );

            // act
            ResponseEntity<ApiResponse<LikeV1Dto.LikeResponse>> response = testRestTemplate.exchange(
                "/api/v1/products/" + product.getId() + "/likes",
                HttpMethod.POST,
                new HttpEntity<>(createAuthHeaders(LOGIN_ID)),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().likeCount()).isEqualTo(1)
            );
        }

        @DisplayName("존재하지 않는 productId로 요청하면, 404 Not Found를 반환한다.")
        @Test
        void returns404_whenProductNotFound() {
            // arrange
            createUser(LOGIN_ID);

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/products/99999/likes",
                HttpMethod.POST,
                new HttpEntity<>(createAuthHeaders(LOGIN_ID)),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @DisplayName("DELETE /api/v1/products/{productId}/likes")
    @Nested
    class UnlikeProduct {

        @DisplayName("인증 없이 요청하면, 401 Unauthorized를 반환한다.")
        @Test
        void returns401_whenNoAuth() {
            // arrange
            Product product = createProduct("에어맥스");

            // act
            ResponseEntity<Void> response = testRestTemplate.exchange(
                "/api/v1/products/" + product.getId() + "/likes",
                HttpMethod.DELETE,
                new HttpEntity<>(null),
                Void.class
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @DisplayName("좋아요 후 취소하면 204 No Content를 반환하고 likeCount가 0이 된다.")
        @Test
        void returns204AndLikeCountZero_whenUnliked() {
            // arrange
            createUser(LOGIN_ID);
            Product product = createProduct("에어맥스");
            testRestTemplate.exchange(
                "/api/v1/products/" + product.getId() + "/likes",
                HttpMethod.POST,
                new HttpEntity<>(createAuthHeaders(LOGIN_ID)),
                new ParameterizedTypeReference<ApiResponse<LikeV1Dto.LikeResponse>>() {}
            );

            // act
            ResponseEntity<Void> response = testRestTemplate.exchange(
                "/api/v1/products/" + product.getId() + "/likes",
                HttpMethod.DELETE,
                new HttpEntity<>(createAuthHeaders(LOGIN_ID)),
                Void.class
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

            ResponseEntity<ApiResponse<ProductV1Dto.ProductResponse>> productResponse = testRestTemplate.exchange(
                "/api/v1/products/" + product.getId(),
                HttpMethod.GET,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );
            assertThat(productResponse.getBody().data().likeCount()).isEqualTo(0);
        }

        @DisplayName("좋아요 없는 상품에 취소 요청해도 204 No Content를 반환한다 (멱등).")
        @Test
        void returns204_whenUnlikeCalledWithoutLike() {
            // arrange
            createUser(LOGIN_ID);
            Product product = createProduct("에어맥스");

            // act
            ResponseEntity<Void> response = testRestTemplate.exchange(
                "/api/v1/products/" + product.getId() + "/likes",
                HttpMethod.DELETE,
                new HttpEntity<>(createAuthHeaders(LOGIN_ID)),
                Void.class
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        @DisplayName("존재하지 않는 productId로 요청하면, 404 Not Found를 반환한다.")
        @Test
        void returns404_whenProductNotFound() {
            // arrange
            createUser(LOGIN_ID);

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/products/99999/likes",
                HttpMethod.DELETE,
                new HttpEntity<>(createAuthHeaders(LOGIN_ID)),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @DisplayName("GET /api/v1/users/{userId}/likes")
    @Nested
    class GetLikedProducts {

        @DisplayName("인증 없이 요청하면, 401 Unauthorized를 반환한다.")
        @Test
        void returns401_whenNoAuth() {
            // arrange
            UserModel user = createUser(LOGIN_ID);

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/users/" + user.getId() + "/likes",
                HttpMethod.GET,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @DisplayName("좋아요한 상품 목록을 조회하면 200 OK와 isLiked=true인 상품 목록을 반환한다.")
        @Test
        void returns200WithLikedProducts_whenUserHasLikes() {
            // arrange
            UserModel user = createUser(LOGIN_ID);
            Product product = createProduct("에어맥스");
            testRestTemplate.exchange(
                "/api/v1/products/" + product.getId() + "/likes",
                HttpMethod.POST,
                new HttpEntity<>(createAuthHeaders(LOGIN_ID)),
                new ParameterizedTypeReference<ApiResponse<LikeV1Dto.LikeResponse>>() {}
            );

            // act
            ResponseEntity<ApiResponse<LikeV1Dto.LikedProductPageResponse>> response = testRestTemplate.exchange(
                "/api/v1/users/" + user.getId() + "/likes?page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(LOGIN_ID)),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data().content()).hasSize(1),
                () -> assertThat(response.getBody().data().content().get(0).isLiked()).isTrue()
            );
        }

        @DisplayName("다른 사용자의 좋아요 목록 조회 시 403 Forbidden을 반환한다.")
        @Test
        void returns403_whenAccessingOtherUsersLikes() {
            // arrange
            createUser(LOGIN_ID);
            UserModel otherUser = createUser(OTHER_LOGIN_ID);

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/users/" + otherUser.getId() + "/likes?page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(LOGIN_ID)),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @DisplayName("다른 사용자의 좋아요는 본인 목록에 포함되지 않는다.")
        @Test
        void excludesOtherUsersLikes() {
            // arrange
            UserModel user = createUser(LOGIN_ID);
            createUser(OTHER_LOGIN_ID);
            Product product1 = createProduct("에어맥스");
            Product product2 = createProduct("에어조던");

            testRestTemplate.exchange(
                "/api/v1/products/" + product1.getId() + "/likes",
                HttpMethod.POST,
                new HttpEntity<>(createAuthHeaders(LOGIN_ID)),
                new ParameterizedTypeReference<ApiResponse<LikeV1Dto.LikeResponse>>() {}
            );
            testRestTemplate.exchange(
                "/api/v1/products/" + product2.getId() + "/likes",
                HttpMethod.POST,
                new HttpEntity<>(createAuthHeaders(OTHER_LOGIN_ID)),
                new ParameterizedTypeReference<ApiResponse<LikeV1Dto.LikeResponse>>() {}
            );

            // act
            ResponseEntity<ApiResponse<LikeV1Dto.LikedProductPageResponse>> response = testRestTemplate.exchange(
                "/api/v1/users/" + user.getId() + "/likes?page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(LOGIN_ID)),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().content()).hasSize(1),
                () -> assertThat(response.getBody().data().content().get(0).id()).isEqualTo(product1.getId())
            );
        }
    }

    @DisplayName("GET /api/v1/products/{productId} + isLiked")
    @Nested
    class GetProductWithIsLiked {

        @DisplayName("비로그인 상태로 조회하면 isLiked가 null이다.")
        @Test
        void returnsIsLikedNull_whenNotAuthenticated() {
            // arrange
            Product product = createProduct("에어맥스");

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
                () -> assertThat(response.getBody().data().isLiked()).isNull()
            );
        }

        @DisplayName("로그인 후 좋아요하지 않은 상품 조회 시 isLiked가 false이다.")
        @Test
        void returnsIsLikedFalse_whenLoggedInButNotLiked() {
            // arrange
            createUser(LOGIN_ID);
            Product product = createProduct("에어맥스");

            // act
            ResponseEntity<ApiResponse<ProductV1Dto.ProductResponse>> response = testRestTemplate.exchange(
                "/api/v1/products/" + product.getId(),
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(LOGIN_ID)),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().isLiked()).isFalse()
            );
        }

        @DisplayName("로그인 후 좋아요한 상품 조회 시 isLiked가 true이다.")
        @Test
        void returnsIsLikedTrue_whenLoggedInAndLiked() {
            // arrange
            createUser(LOGIN_ID);
            Product product = createProduct("에어맥스");
            testRestTemplate.exchange(
                "/api/v1/products/" + product.getId() + "/likes",
                HttpMethod.POST,
                new HttpEntity<>(createAuthHeaders(LOGIN_ID)),
                new ParameterizedTypeReference<ApiResponse<LikeV1Dto.LikeResponse>>() {}
            );

            // act
            ResponseEntity<ApiResponse<ProductV1Dto.ProductResponse>> response = testRestTemplate.exchange(
                "/api/v1/products/" + product.getId(),
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(LOGIN_ID)),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().isLiked()).isTrue()
            );
        }

        @DisplayName("잘못된 인증 헤더로 조회하면 401 Unauthorized를 반환한다.")
        @Test
        void returns401_whenInvalidAuthHeader() {
            // arrange
            Product product = createProduct("에어맥스");
            HttpHeaders invalidHeaders = new HttpHeaders();
            invalidHeaders.set("X-Loopers-LoginId", "nonexistent");
            invalidHeaders.set("X-Loopers-LoginPw", "wrongpassword");

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/products/" + product.getId(),
                HttpMethod.GET,
                new HttpEntity<>(invalidHeaders),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }
}
