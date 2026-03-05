package com.loopers.interfaces.api;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.SellingStatus;
import com.loopers.domain.user.PasswordEncoder;
import com.loopers.domain.user.UserModel;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.order.OrderJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.interfaces.api.order.OrderV1Dto;
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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderV1ApiE2ETest {

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
    private OrderJpaRepository orderJpaRepository;

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

    private ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> placeOrder(String loginId, Long productId, int quantity) {
        OrderV1Dto.CreateRequest request = new OrderV1Dto.CreateRequest(productId, quantity);
        return testRestTemplate.exchange(
            "/api/v1/orders",
            HttpMethod.POST,
            new HttpEntity<>(request, createAuthHeaders(loginId)),
            new ParameterizedTypeReference<>() {}
        );
    }

    @DisplayName("POST /api/v1/orders")
    @Nested
    class CreateOrder {

        @DisplayName("인증 없이 요청하면, 401 Unauthorized를 반환한다.")
        @Test
        void returns401_whenNoAuth() {
            // arrange
            Product product = createProduct("에어맥스", 10000, 10, SellingStatus.SELLING);
            OrderV1Dto.CreateRequest request = new OrderV1Dto.CreateRequest(product.getId(), 1);

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @DisplayName("존재하지 않는 productId로 주문하면, 404 Not Found를 반환한다.")
        @Test
        void returns404_whenProductNotFound() {
            // arrange
            createUser(LOGIN_ID);
            OrderV1Dto.CreateRequest request = new OrderV1Dto.CreateRequest(99999L, 1);

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(request, createAuthHeaders(LOGIN_ID)),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @DisplayName("판매 중단(STOP) 상품을 주문하면, 400 Bad Request를 반환한다.")
        @Test
        void returns400_whenProductIsNotSelling() {
            // arrange
            createUser(LOGIN_ID);
            Product product = createProduct("단종상품", 10000, 10, SellingStatus.STOP);

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(new OrderV1Dto.CreateRequest(product.getId(), 1), createAuthHeaders(LOGIN_ID)),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @DisplayName("재고를 초과하는 수량으로 주문하면, 400 Bad Request를 반환한다.")
        @Test
        void returns400_whenQuantityExceedsStock() {
            // arrange
            createUser(LOGIN_ID);
            Product product = createProduct("에어맥스", 10000, 2, SellingStatus.SELLING);

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(new OrderV1Dto.CreateRequest(product.getId(), 5), createAuthHeaders(LOGIN_ID)),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @DisplayName("정상 주문이면, 201 Created와 OrderResponse를 반환하고 재고가 차감된다.")
        @Test
        void returns201WithOrderResponse_andDeductsStock() {
            // arrange
            createUser(LOGIN_ID);
            Product product = createProduct("에어맥스", 10000, 10, SellingStatus.SELLING);

            // act
            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response = placeOrder(LOGIN_ID, product.getId(), 3);

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data().status()).isEqualTo("ORDERED"),
                () -> assertThat(response.getBody().data().totalPrice()).isEqualTo(30000),
                () -> assertThat(response.getBody().data().items()).hasSize(1),
                () -> assertThat(response.getBody().data().items().get(0).productId()).isEqualTo(product.getId()),
                () -> assertThat(response.getBody().data().items().get(0).quantity()).isEqualTo(3),
                () -> assertThat(response.getBody().data().items().get(0).unitPrice()).isEqualTo(10000),
                () -> assertThat(response.getBody().data().items().get(0).subtotal()).isEqualTo(30000)
            );

            Product updated = productJpaRepository.findByIdAndDeletedAtIsNull(product.getId()).orElseThrow();
            assertThat(updated.getStock()).isEqualTo(7);
        }
    }

    @DisplayName("GET /api/v1/orders/{orderId}")
    @Nested
    class GetOrder {

        @DisplayName("인증 없이 요청하면, 401 Unauthorized를 반환한다.")
        @Test
        void returns401_whenNoAuth() {
            // arrange
            createUser(LOGIN_ID);
            Product product = createProduct("에어맥스", 10000, 10, SellingStatus.SELLING);
            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> created = placeOrder(LOGIN_ID, product.getId(), 1);
            Long orderId = created.getBody().data().id();

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/orders/" + orderId,
                HttpMethod.GET,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @DisplayName("본인 주문을 조회하면, 200 OK와 OrderResponse를 반환한다.")
        @Test
        void returns200WithOrderResponse_whenOwnerRequests() {
            // arrange
            createUser(LOGIN_ID);
            Product product = createProduct("에어맥스", 10000, 10, SellingStatus.SELLING);
            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> created = placeOrder(LOGIN_ID, product.getId(), 2);
            Long orderId = created.getBody().data().id();

            // act
            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response = testRestTemplate.exchange(
                "/api/v1/orders/" + orderId,
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(LOGIN_ID)),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().id()).isEqualTo(orderId),
                () -> assertThat(response.getBody().data().status()).isEqualTo("ORDERED"),
                () -> assertThat(response.getBody().data().totalPrice()).isEqualTo(20000)
            );
        }

        @DisplayName("다른 사용자의 주문을 조회하면, 404 Not Found를 반환한다.")
        @Test
        void returns404_whenOtherUserRequests() {
            // arrange
            createUser(LOGIN_ID);
            createUser(OTHER_LOGIN_ID);
            Product product = createProduct("에어맥스", 10000, 10, SellingStatus.SELLING);
            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> created = placeOrder(LOGIN_ID, product.getId(), 1);
            Long orderId = created.getBody().data().id();

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/orders/" + orderId,
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(OTHER_LOGIN_ID)),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @DisplayName("존재하지 않는 orderId로 조회하면, 404 Not Found를 반환한다.")
        @Test
        void returns404_whenOrderNotFound() {
            // arrange
            createUser(LOGIN_ID);

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/orders/99999",
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(LOGIN_ID)),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @DisplayName("GET /api/v1/orders")
    @Nested
    class GetMyOrders {

        private String buildOrdersUrl(ZonedDateTime startAt, ZonedDateTime endAt) {
            String start = startAt.withZoneSameInstant(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
            String end = endAt.withZoneSameInstant(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
            return "/api/v1/orders?startAt=" + start + "&endAt=" + end;
        }

        @DisplayName("인증 없이 요청하면, 401 Unauthorized를 반환한다.")
        @Test
        void returns401_whenNoAuth() {
            // arrange
            ZonedDateTime now = ZonedDateTime.now();
            String url = buildOrdersUrl(now.minusDays(1), now.plusDays(1));

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @DisplayName("본인 주문 목록을 조회하면, 200 OK와 본인 주문만 반환한다.")
        @Test
        void returns200WithOnlyUserOrders() {
            // arrange
            createUser(LOGIN_ID);
            createUser(OTHER_LOGIN_ID);
            Product product = createProduct("에어맥스", 10000, 10, SellingStatus.SELLING);
            placeOrder(LOGIN_ID, product.getId(), 1);
            placeOrder(LOGIN_ID, product.getId(), 1);
            placeOrder(OTHER_LOGIN_ID, product.getId(), 1);

            ZonedDateTime now = ZonedDateTime.now();
            String url = buildOrdersUrl(now.minusDays(1), now.plusDays(1));

            // act
            ResponseEntity<ApiResponse<List<OrderV1Dto.OrderResponse>>> response = testRestTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(LOGIN_ID)),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data()).hasSize(2)
            );
        }
    }

    @DisplayName("PATCH /api/v1/orders/{orderId}/cancel")
    @Nested
    class CancelOrder {

        @DisplayName("인증 없이 요청하면, 401 Unauthorized를 반환한다.")
        @Test
        void returns401_whenNoAuth() {
            // arrange
            createUser(LOGIN_ID);
            Product product = createProduct("에어맥스", 10000, 10, SellingStatus.SELLING);
            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> created = placeOrder(LOGIN_ID, product.getId(), 1);
            Long orderId = created.getBody().data().id();

            // act
            ResponseEntity<Void> response = testRestTemplate.exchange(
                "/api/v1/orders/" + orderId + "/cancel",
                HttpMethod.PATCH,
                new HttpEntity<>(null),
                Void.class
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @DisplayName("정상 취소이면, 204 No Content를 반환하고 재고가 복원된다.")
        @Test
        void returns204AndRestoresStock_whenCancelSucceeds() {
            // arrange
            createUser(LOGIN_ID);
            Product product = createProduct("에어맥스", 10000, 10, SellingStatus.SELLING);
            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> created = placeOrder(LOGIN_ID, product.getId(), 3);
            Long orderId = created.getBody().data().id();

            // act
            ResponseEntity<Void> response = testRestTemplate.exchange(
                "/api/v1/orders/" + orderId + "/cancel",
                HttpMethod.PATCH,
                new HttpEntity<>(createAuthHeaders(LOGIN_ID)),
                Void.class
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

            Product restored = productJpaRepository.findByIdAndDeletedAtIsNull(product.getId()).orElseThrow();
            assertThat(restored.getStock()).isEqualTo(10);
        }

        @DisplayName("다른 사용자의 주문을 취소하려 하면, 404 Not Found를 반환한다.")
        @Test
        void returns404_whenOtherUserTriesToCancel() {
            // arrange
            createUser(LOGIN_ID);
            createUser(OTHER_LOGIN_ID);
            Product product = createProduct("에어맥스", 10000, 10, SellingStatus.SELLING);
            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> created = placeOrder(LOGIN_ID, product.getId(), 1);
            Long orderId = created.getBody().data().id();

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/orders/" + orderId + "/cancel",
                HttpMethod.PATCH,
                new HttpEntity<>(createAuthHeaders(OTHER_LOGIN_ID)),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @DisplayName("이미 취소된 주문을 다시 취소하면, 400 Bad Request를 반환한다.")
        @Test
        void returns400_whenOrderAlreadyCancelled() {
            // arrange
            createUser(LOGIN_ID);
            Product product = createProduct("에어맥스", 10000, 10, SellingStatus.SELLING);
            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> created = placeOrder(LOGIN_ID, product.getId(), 1);
            Long orderId = created.getBody().data().id();
            testRestTemplate.exchange(
                "/api/v1/orders/" + orderId + "/cancel",
                HttpMethod.PATCH,
                new HttpEntity<>(createAuthHeaders(LOGIN_ID)),
                Void.class
            );

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/orders/" + orderId + "/cancel",
                HttpMethod.PATCH,
                new HttpEntity<>(createAuthHeaders(LOGIN_ID)),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @DisplayName("배송 완료된 주문을 취소하면, 400 Bad Request를 반환한다.")
        @Test
        void returns400_whenOrderIsDelivered() {
            // arrange
            createUser(LOGIN_ID);
            Product product = createProduct("에어맥스", 10000, 10, SellingStatus.SELLING);
            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> created = placeOrder(LOGIN_ID, product.getId(), 1);
            Long orderId = created.getBody().data().id();

            Order order = orderJpaRepository.findByIdAndDeletedAtIsNull(orderId).orElseThrow();
            order.changeStatus(OrderStatus.DELIVERED);
            orderJpaRepository.save(order);

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/orders/" + orderId + "/cancel",
                HttpMethod.PATCH,
                new HttpEntity<>(createAuthHeaders(LOGIN_ID)),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @DisplayName("존재하지 않는 orderId로 취소하면, 404 Not Found를 반환한다.")
        @Test
        void returns404_whenOrderNotFound() {
            // arrange
            createUser(LOGIN_ID);

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/orders/99999/cancel",
                HttpMethod.PATCH,
                new HttpEntity<>(createAuthHeaders(LOGIN_ID)),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
