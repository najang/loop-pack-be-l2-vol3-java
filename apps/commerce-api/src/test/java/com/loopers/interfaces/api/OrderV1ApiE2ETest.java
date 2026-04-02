package com.loopers.interfaces.api;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.coupon.CouponTemplate;
import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.SellingStatus;
import com.loopers.domain.user.PasswordEncoder;
import com.loopers.domain.user.UserModel;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.coupon.CouponTemplateJpaRepository;
import com.loopers.infrastructure.coupon.UserCouponJpaRepository;
import com.loopers.infrastructure.order.OrderJpaRepository;
import com.loopers.infrastructure.outbox.OutboxEventJpaRepository;
import com.loopers.infrastructure.payment.PaymentJpaRepository;
import com.loopers.infrastructure.pg.PgGateway;
import com.loopers.infrastructure.pg.PgPaymentResponse;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.interfaces.api.order.OrderV1Dto;
import com.loopers.interfaces.api.payment.PaymentV1Dto;
import com.loopers.domain.queue.EntryTokenService;
import com.loopers.support.auth.QueueEntryTokenInterceptor;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderV1ApiE2ETest {

    private static final String LOGIN_ID = "testuser1";
    private static final String OTHER_LOGIN_ID = "otheruser1";
    private static final String RAW_PASSWORD = "Test1234!";
    private static final String CARD_TYPE = "SAMSUNG";
    private static final String CARD_NO = "1234-5678-9012-3456";

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
    private PaymentJpaRepository paymentJpaRepository;

    @Autowired
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @Autowired
    private CouponTemplateJpaRepository couponTemplateJpaRepository;

    @Autowired
    private UserCouponJpaRepository userCouponJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private EntryTokenService entryTokenService;

    @Autowired
    private RedisCleanUp redisCleanUp;

    @MockBean
    private PgGateway pgGateway;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
        redisCleanUp.truncateAll();
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

    private HttpHeaders createOrderAuthHeaders(String loginId) {
        UserModel user = userJpaRepository.findByLoginIdValue(loginId).orElseThrow();
        String token = entryTokenService.issue(user.getId());
        HttpHeaders headers = createAuthHeaders(loginId);
        headers.set(QueueEntryTokenInterceptor.HEADER_ENTRY_TOKEN, token);
        return headers;
    }

    private Product createProduct(String name, int price, int stock, SellingStatus status) {
        Brand brand = brandJpaRepository.save(new Brand("Nike", null));
        return productJpaRepository.save(new Product(brand.getId(), name, null, price, stock, status));
    }

    private ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> placeOrder(String loginId, Long productId, int quantity, Long couponId) {
        OrderV1Dto.CreateRequest request = new OrderV1Dto.CreateRequest(productId, quantity, couponId, CARD_TYPE, CARD_NO);
        return testRestTemplate.exchange(
            "/api/v1/orders",
            HttpMethod.POST,
            new HttpEntity<>(request, createOrderAuthHeaders(loginId)),
            new ParameterizedTypeReference<>() {}
        );
    }

    private ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> placeOrder(String loginId, Long productId, int quantity) {
        return placeOrder(loginId, productId, quantity, null);
    }

    private UserCoupon createUserCoupon(Long userId, int discountValue, CouponType type) {
        CouponTemplate template = couponTemplateJpaRepository.save(
            new CouponTemplate("할인쿠폰", type, discountValue, null, ZonedDateTime.now().plusDays(7))
        );
        return userCouponJpaRepository.save(new UserCoupon(userId, template.getId()));
    }

    private void mockPgGatewaySuccess() {
        when(pgGateway.requestPayment(any(), any()))
            .thenReturn(new PgPaymentResponse("TX-001"));
    }

    @DisplayName("POST /api/v1/orders")
    @Nested
    class CreateOrder {

        @DisplayName("인증 없이 요청하면, 401 Unauthorized를 반환한다.")
        @Test
        void returns401_whenNoAuth() {
            // arrange
            Product product = createProduct("에어맥스", 10000, 10, SellingStatus.SELLING);
            OrderV1Dto.CreateRequest request = new OrderV1Dto.CreateRequest(product.getId(), 1, null, CARD_TYPE, CARD_NO);

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
            mockPgGatewaySuccess();
            OrderV1Dto.CreateRequest request = new OrderV1Dto.CreateRequest(99999L, 1, null, CARD_TYPE, CARD_NO);

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(request, createOrderAuthHeaders(LOGIN_ID)),
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
            mockPgGatewaySuccess();
            Product product = createProduct("단종상품", 10000, 10, SellingStatus.STOP);

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(new OrderV1Dto.CreateRequest(product.getId(), 1, null, CARD_TYPE, CARD_NO), createOrderAuthHeaders(LOGIN_ID)),
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
            mockPgGatewaySuccess();
            Product product = createProduct("에어맥스", 10000, 2, SellingStatus.SELLING);

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(new OrderV1Dto.CreateRequest(product.getId(), 5, null, CARD_TYPE, CARD_NO), createOrderAuthHeaders(LOGIN_ID)),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @DisplayName("쿠폰 없이 정상 주문이면, 202 Accepted와 OrderResponse(PAYMENT_PENDING)를 반환한다.")
        @Test
        void returns202WithPaymentPendingStatus_andDeductsStock() {
            // arrange
            createUser(LOGIN_ID);
            mockPgGatewaySuccess();
            Product product = createProduct("에어맥스", 10000, 10, SellingStatus.SELLING);

            // act
            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response = placeOrder(LOGIN_ID, product.getId(), 3);

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data().status()).isEqualTo("PAYMENT_PENDING"),
                () -> assertThat(response.getBody().data().originalTotalPrice()).isEqualTo(30000),
                () -> assertThat(response.getBody().data().discountAmount()).isEqualTo(0),
                () -> assertThat(response.getBody().data().finalTotalPrice()).isEqualTo(30000),
                () -> assertThat(response.getBody().data().userCouponId()).isNull(),
                () -> assertThat(response.getBody().data().items()).hasSize(1),
                () -> assertThat(response.getBody().data().items().get(0).productId()).isEqualTo(product.getId()),
                () -> assertThat(response.getBody().data().items().get(0).quantity()).isEqualTo(3),
                () -> assertThat(response.getBody().data().items().get(0).unitPrice()).isEqualTo(10000),
                () -> assertThat(response.getBody().data().items().get(0).subtotal()).isEqualTo(30000)
            );

            Product updated = productJpaRepository.findByIdAndDeletedAtIsNull(product.getId()).orElseThrow();
            assertThat(updated.getStock()).isEqualTo(7);
        }

        @DisplayName("FIXED 쿠폰을 적용하면, 202 Accepted와 할인이 반영된 OrderResponse를 반환한다.")
        @Test
        void returns202WithDiscount_whenFixedCouponApplied() {
            // arrange
            UserModel user = createUser(LOGIN_ID);
            mockPgGatewaySuccess();
            Product product = createProduct("에어맥스", 10000, 10, SellingStatus.SELLING);
            UserCoupon userCoupon = createUserCoupon(user.getId(), 3000, CouponType.FIXED);

            // act
            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response = placeOrder(LOGIN_ID, product.getId(), 2, userCoupon.getId());

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED),
                () -> assertThat(response.getBody().data().originalTotalPrice()).isEqualTo(20000),
                () -> assertThat(response.getBody().data().discountAmount()).isEqualTo(3000),
                () -> assertThat(response.getBody().data().finalTotalPrice()).isEqualTo(17000),
                () -> assertThat(response.getBody().data().userCouponId()).isEqualTo(userCoupon.getId())
            );
        }

        @DisplayName("이미 사용된 쿠폰으로 주문하면, 400 Bad Request를 반환한다.")
        @Test
        void returns400_whenCouponAlreadyUsed() {
            // arrange
            UserModel user = createUser(LOGIN_ID);
            mockPgGatewaySuccess();
            Product product = createProduct("에어맥스", 10000, 10, SellingStatus.SELLING);
            UserCoupon userCoupon = createUserCoupon(user.getId(), 1000, CouponType.FIXED);
            placeOrder(LOGIN_ID, product.getId(), 1, userCoupon.getId());

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(new OrderV1Dto.CreateRequest(product.getId(), 1, userCoupon.getId(), CARD_TYPE, CARD_NO), createOrderAuthHeaders(LOGIN_ID)),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @DisplayName("타 유저의 쿠폰으로 주문하면, 400 Bad Request를 반환한다.")
        @Test
        void returns400_whenCouponBelongsToOtherUser() {
            // arrange
            createUser(LOGIN_ID);
            UserModel otherUser = createUser(OTHER_LOGIN_ID);
            mockPgGatewaySuccess();
            Product product = createProduct("에어맥스", 10000, 10, SellingStatus.SELLING);
            UserCoupon otherUserCoupon = createUserCoupon(otherUser.getId(), 1000, CouponType.FIXED);

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(new OrderV1Dto.CreateRequest(product.getId(), 1, otherUserCoupon.getId(), CARD_TYPE, CARD_NO), createOrderAuthHeaders(LOGIN_ID)),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @DisplayName("성공 콜백 수신 후 payment-events OutboxEvent(COMPLETED)가 저장된다.")
        @Test
        void savesCompletedOutboxEvent_afterSuccessCallback() {
            // arrange
            UserModel user = createUser(LOGIN_ID);
            mockPgGatewaySuccess();
            Product product = createProduct("에어맥스", 10000, 10, SellingStatus.SELLING);
            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> created = placeOrder(LOGIN_ID, product.getId(), 1);
            Long orderId = created.getBody().data().id();

            Long paymentId = paymentJpaRepository.findByOrderId(orderId).orElseThrow().getId();

            // act
            PaymentV1Dto.CallbackRequest callbackRequest = new PaymentV1Dto.CallbackRequest("TX-001", "SUCCESS", null);
            testRestTemplate.exchange(
                "/api/v1/payments/" + paymentId + "/callback",
                HttpMethod.POST,
                new HttpEntity<>(callbackRequest),
                Void.class
            );

            // assert
            boolean hasPaymentEvent = outboxEventJpaRepository.findByPublishedFalseOrderByCreatedAtAsc().stream()
                .anyMatch(e -> "payment-events".equals(e.getTopic()) && e.getPayload().contains("\"status\":\"COMPLETED\""));
            assertThat(hasPaymentEvent).isTrue();
        }

        @DisplayName("실패 콜백 수신 후 payment-events OutboxEvent(FAILED)가 저장된다.")
        @Test
        void savesFailedOutboxEvent_afterFailCallback() {
            // arrange
            UserModel user = createUser(LOGIN_ID);
            mockPgGatewaySuccess();
            Product product = createProduct("에어맥스", 10000, 10, SellingStatus.SELLING);
            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> created = placeOrder(LOGIN_ID, product.getId(), 3);
            Long orderId = created.getBody().data().id();

            Long paymentId = paymentJpaRepository.findByOrderId(orderId).orElseThrow().getId();

            // act
            PaymentV1Dto.CallbackRequest callbackRequest = new PaymentV1Dto.CallbackRequest("TX-001", "FAILED", "카드 한도 초과");
            testRestTemplate.exchange(
                "/api/v1/payments/" + paymentId + "/callback",
                HttpMethod.POST,
                new HttpEntity<>(callbackRequest),
                Void.class
            );

            // assert
            boolean hasPaymentEvent = outboxEventJpaRepository.findByPublishedFalseOrderByCreatedAtAsc().stream()
                .anyMatch(e -> "payment-events".equals(e.getTopic()) && e.getPayload().contains("\"status\":\"FAILED\""));
            assertThat(hasPaymentEvent).isTrue();
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
            mockPgGatewaySuccess();
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
            mockPgGatewaySuccess();
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
                () -> assertThat(response.getBody().data().status()).isEqualTo("PAYMENT_PENDING"),
                () -> assertThat(response.getBody().data().finalTotalPrice()).isEqualTo(20000)
            );
        }

        @DisplayName("다른 사용자의 주문을 조회하면, 404 Not Found를 반환한다.")
        @Test
        void returns404_whenOtherUserRequests() {
            // arrange
            createUser(LOGIN_ID);
            createUser(OTHER_LOGIN_ID);
            mockPgGatewaySuccess();
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
            mockPgGatewaySuccess();
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
            mockPgGatewaySuccess();
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

        @DisplayName("ORDERED 상태 주문을 정상 취소하면, 204 No Content를 반환하고 재고가 복원된다.")
        @Test
        void returns204AndRestoresStock_whenCancelSucceeds() {
            // arrange
            createUser(LOGIN_ID);
            mockPgGatewaySuccess();
            Product product = createProduct("에어맥스", 10000, 10, SellingStatus.SELLING);
            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> created = placeOrder(LOGIN_ID, product.getId(), 3);
            Long orderId = created.getBody().data().id();

            Order order = orderJpaRepository.findByIdAndDeletedAtIsNull(orderId).orElseThrow();
            order.confirmPayment();
            orderJpaRepository.save(order);

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
            mockPgGatewaySuccess();
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
            mockPgGatewaySuccess();
            Product product = createProduct("에어맥스", 10000, 10, SellingStatus.SELLING);
            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> created = placeOrder(LOGIN_ID, product.getId(), 1);
            Long orderId = created.getBody().data().id();

            Order order = orderJpaRepository.findByIdAndDeletedAtIsNull(orderId).orElseThrow();
            order.confirmPayment();
            orderJpaRepository.save(order);

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
            mockPgGatewaySuccess();
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
