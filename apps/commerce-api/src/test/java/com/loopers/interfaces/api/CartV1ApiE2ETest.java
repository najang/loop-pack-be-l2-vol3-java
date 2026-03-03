package com.loopers.interfaces.api;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.SellingStatus;
import com.loopers.domain.user.PasswordEncoder;
import com.loopers.domain.user.UserModel;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.cart.CartJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.interfaces.api.cart.CartV1Dto;
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
class CartV1ApiE2ETest {

    private static final String LOGIN_ID = "cartuser1";
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
    private CartJpaRepository cartJpaRepository;

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

    private Product createProduct(String name, int price, int stock, SellingStatus status) {
        Brand brand = brandJpaRepository.save(new Brand("Nike", null));
        return productJpaRepository.save(new Product(brand.getId(), name, null, price, stock, status));
    }

    private ResponseEntity<ApiResponse<CartV1Dto.CartItemResponse>> addCartItem(String loginId, Long productId, int quantity) {
        CartV1Dto.AddRequest request = new CartV1Dto.AddRequest(productId, quantity);
        return testRestTemplate.exchange(
            "/api/v1/cart/items",
            HttpMethod.POST,
            new HttpEntity<>(request, createAuthHeaders(loginId)),
            new ParameterizedTypeReference<>() {}
        );
    }

    @DisplayName("POST /api/v1/cart/items")
    @Nested
    class AddItem {

        @DisplayName("인증 없이 요청하면, 401 Unauthorized를 반환한다.")
        @Test
        void returns401_whenNoAuth() {
            // arrange
            Product product = createProduct("에어맥스", 10000, 10, SellingStatus.SELLING);
            CartV1Dto.AddRequest request = new CartV1Dto.AddRequest(product.getId(), 1);

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/cart/items",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @DisplayName("존재하지 않는 productId면, 404 Not Found를 반환한다.")
        @Test
        void returns404_whenProductNotFound() {
            // arrange
            createUser(LOGIN_ID);
            CartV1Dto.AddRequest request = new CartV1Dto.AddRequest(99999L, 1);

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/cart/items",
                HttpMethod.POST,
                new HttpEntity<>(request, createAuthHeaders(LOGIN_ID)),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @DisplayName("정상 추가 시, 201 Created와 CartItemResponse를 반환한다.")
        @Test
        void returns201WithCartItemResponse_whenAddSucceeds() {
            // arrange
            createUser(LOGIN_ID);
            Product product = createProduct("에어맥스", 10000, 10, SellingStatus.SELLING);

            // act
            ResponseEntity<ApiResponse<CartV1Dto.CartItemResponse>> response = addCartItem(LOGIN_ID, product.getId(), 3);

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data().productId()).isEqualTo(product.getId()),
                () -> assertThat(response.getBody().data().productName()).isEqualTo("에어맥스"),
                () -> assertThat(response.getBody().data().productPrice()).isEqualTo(10000),
                () -> assertThat(response.getBody().data().sellingStatus()).isEqualTo("SELLING"),
                () -> assertThat(response.getBody().data().quantity()).isEqualTo(3),
                () -> assertThat(response.getBody().data().subtotal()).isEqualTo(30000)
            );
        }

        @DisplayName("이미 장바구니에 있는 상품 추가 시, 수량이 누적된다.")
        @Test
        void accumulatesQuantity_whenProductAlreadyInCart() {
            // arrange
            createUser(LOGIN_ID);
            Product product = createProduct("에어맥스", 10000, 10, SellingStatus.SELLING);
            addCartItem(LOGIN_ID, product.getId(), 3);

            // act
            ResponseEntity<ApiResponse<CartV1Dto.CartItemResponse>> response = addCartItem(LOGIN_ID, product.getId(), 2);

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(response.getBody().data().quantity()).isEqualTo(5)
            );
        }
    }

    @DisplayName("GET /api/v1/cart")
    @Nested
    class GetCart {

        @DisplayName("인증 없이 요청하면, 401 Unauthorized를 반환한다.")
        @Test
        void returns401_whenNoAuth() {
            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/cart",
                HttpMethod.GET,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @DisplayName("장바구니가 비어있으면, items=[]이고 totalPrice=0인 CartResponse를 반환한다.")
        @Test
        void returnsEmptyCart_whenNoItemsInCart() {
            // arrange
            createUser(LOGIN_ID);

            // act
            ResponseEntity<ApiResponse<CartV1Dto.CartResponse>> response = testRestTemplate.exchange(
                "/api/v1/cart",
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(LOGIN_ID)),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().items()).isEmpty(),
                () -> assertThat(response.getBody().data().totalPrice()).isEqualTo(0)
            );
        }

        @DisplayName("상품이 있으면, 200 OK와 CartResponse를 반환한다.")
        @Test
        void returnsCartResponse_whenItemsExist() {
            // arrange
            createUser(LOGIN_ID);
            Product product = createProduct("에어맥스", 10000, 10, SellingStatus.SELLING);
            addCartItem(LOGIN_ID, product.getId(), 2);

            // act
            ResponseEntity<ApiResponse<CartV1Dto.CartResponse>> response = testRestTemplate.exchange(
                "/api/v1/cart",
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(LOGIN_ID)),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().items()).hasSize(1),
                () -> assertThat(response.getBody().data().totalPrice()).isEqualTo(20000)
            );
        }
    }

    @DisplayName("PUT /api/v1/cart/items/{productId}")
    @Nested
    class UpdateQuantity {

        @DisplayName("인증 없이 요청하면, 401 Unauthorized를 반환한다.")
        @Test
        void returns401_whenNoAuth() {
            // arrange
            Product product = createProduct("에어맥스", 10000, 10, SellingStatus.SELLING);
            CartV1Dto.UpdateQuantityRequest request = new CartV1Dto.UpdateQuantityRequest(5);

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/cart/items/" + product.getId(),
                HttpMethod.PUT,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @DisplayName("장바구니에 없는 항목이면, 404 Not Found를 반환한다.")
        @Test
        void returns404_whenCartItemNotFound() {
            // arrange
            createUser(LOGIN_ID);
            Product product = createProduct("에어맥스", 10000, 10, SellingStatus.SELLING);
            CartV1Dto.UpdateQuantityRequest request = new CartV1Dto.UpdateQuantityRequest(5);

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/cart/items/" + product.getId(),
                HttpMethod.PUT,
                new HttpEntity<>(request, createAuthHeaders(LOGIN_ID)),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @DisplayName("정상 수량 변경 시, 200 OK와 변경된 CartItemResponse를 반환한다.")
        @Test
        void returns200WithUpdatedCartItemResponse_whenUpdateSucceeds() {
            // arrange
            createUser(LOGIN_ID);
            Product product = createProduct("에어맥스", 10000, 10, SellingStatus.SELLING);
            addCartItem(LOGIN_ID, product.getId(), 3);
            CartV1Dto.UpdateQuantityRequest request = new CartV1Dto.UpdateQuantityRequest(7);

            // act
            ResponseEntity<ApiResponse<CartV1Dto.CartItemResponse>> response = testRestTemplate.exchange(
                "/api/v1/cart/items/" + product.getId(),
                HttpMethod.PUT,
                new HttpEntity<>(request, createAuthHeaders(LOGIN_ID)),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().quantity()).isEqualTo(7),
                () -> assertThat(response.getBody().data().subtotal()).isEqualTo(70000)
            );
        }
    }

    @DisplayName("DELETE /api/v1/cart/items/{productId}")
    @Nested
    class RemoveItem {

        @DisplayName("인증 없이 요청하면, 401 Unauthorized를 반환한다.")
        @Test
        void returns401_whenNoAuth() {
            // arrange
            Product product = createProduct("에어맥스", 10000, 10, SellingStatus.SELLING);

            // act
            ResponseEntity<Void> response = testRestTemplate.exchange(
                "/api/v1/cart/items/" + product.getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(null),
                Void.class
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @DisplayName("장바구니에 없는 항목이면, 404 Not Found를 반환한다.")
        @Test
        void returns404_whenCartItemNotFound() {
            // arrange
            createUser(LOGIN_ID);
            Product product = createProduct("에어맥스", 10000, 10, SellingStatus.SELLING);

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/cart/items/" + product.getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(createAuthHeaders(LOGIN_ID)),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @DisplayName("정상 삭제 시, 204 No Content를 반환한다.")
        @Test
        void returns204_whenDeleteSucceeds() {
            // arrange
            createUser(LOGIN_ID);
            Product product = createProduct("에어맥스", 10000, 10, SellingStatus.SELLING);
            addCartItem(LOGIN_ID, product.getId(), 1);

            // act
            ResponseEntity<Void> response = testRestTemplate.exchange(
                "/api/v1/cart/items/" + product.getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(createAuthHeaders(LOGIN_ID)),
                Void.class
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }
    }
}
