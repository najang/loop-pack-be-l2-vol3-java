package com.loopers.interfaces.api;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.SellingStatus;
import com.loopers.domain.user.PasswordEncoder;
import com.loopers.domain.user.UserModel;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.outbox.OutboxEventJpaRepository;
import com.loopers.infrastructure.payment.PaymentJpaRepository;
import com.loopers.infrastructure.pg.PgGateway;
import com.loopers.infrastructure.pg.PgPaymentResponse;
import com.loopers.infrastructure.pg.PgPaymentStatusResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentAdminV1ApiE2ETest {

    private static final String ADMIN_HEADER = "X-Loopers-Ldap";
    private static final String ADMIN_VALUE = "loopers.admin";
    private static final String LOGIN_ID = "testpaymentadmin1";
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
    private PaymentJpaRepository paymentJpaRepository;

    @Autowired
    private OutboxEventJpaRepository outboxEventJpaRepository;

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

    private UserModel createUser() {
        return userJpaRepository.save(new UserModel(
            LOGIN_ID,
            passwordEncoder.encode(RAW_PASSWORD),
            "홍길동",
            LocalDate.of(1990, 1, 15),
            LOGIN_ID + "@example.com"
        ));
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Loopers-LoginId", LOGIN_ID);
        headers.set("X-Loopers-LoginPw", RAW_PASSWORD);
        return headers;
    }

    private HttpHeaders createOrderAuthHeaders() {
        UserModel user = userJpaRepository.findByLoginIdValue(LOGIN_ID).orElseThrow();
        String token = entryTokenService.issue(user.getId());
        HttpHeaders headers = createAuthHeaders();
        headers.set(QueueEntryTokenInterceptor.HEADER_ENTRY_TOKEN, token);
        return headers;
    }

    private HttpHeaders createAdminHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(ADMIN_HEADER, ADMIN_VALUE);
        return headers;
    }

    private Product createProduct(int price, int stock) {
        Brand brand = brandJpaRepository.save(new Brand("Nike", null));
        return productJpaRepository.save(new Product(brand.getId(), "에어맥스", null, price, stock, SellingStatus.SELLING));
    }

    private Long placeOrderAndGetPaymentId(Long productId) {
        when(pgGateway.requestPayment(any(), any())).thenReturn(new PgPaymentResponse("TX-001"));
        OrderV1Dto.CreateRequest request = new OrderV1Dto.CreateRequest(productId, 1, null, CARD_TYPE, CARD_NO);
        ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response = testRestTemplate.exchange(
            "/api/v1/orders",
            HttpMethod.POST,
            new HttpEntity<>(request, createOrderAuthHeaders()),
            new ParameterizedTypeReference<>() {}
        );
        Long orderId = response.getBody().data().id();
        return paymentJpaRepository.findByOrderId(orderId).orElseThrow().getId();
    }

    @DisplayName("POST /api-admin/v1/payments/{paymentId}/sync")
    @Nested
    class SyncPayment {

        @DisplayName("어드민 헤더 없이 요청하면, 401 Unauthorized를 반환한다.")
        @Test
        void returns401_whenNoAdminHeader() {
            // arrange
            createUser();
            Product product = createProduct(10000, 10);
            Long paymentId = placeOrderAndGetPaymentId(product.getId());

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api-admin/v1/payments/" + paymentId + "/sync",
                HttpMethod.POST,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @DisplayName("존재하지 않는 결제 ID로 요청하면, 404 Not Found를 반환한다.")
        @Test
        void returns404_whenPaymentNotFound() {
            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api-admin/v1/payments/99999/sync",
                HttpMethod.POST,
                new HttpEntity<>(null, createAdminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @DisplayName("PG가 COMPLETED를 반환하면, 200 OK와 COMPLETED 상태를 반환하고 payment-events OutboxEvent가 저장된다.")
        @Test
        void returns200WithCompletedStatus_andOutboxEventSaved_whenPgReportsCompleted() {
            // arrange
            createUser();
            Product product = createProduct(10000, 10);
            Long paymentId = placeOrderAndGetPaymentId(product.getId());

            when(pgGateway.inquirePayment(any()))
                .thenReturn(new PgPaymentStatusResponse("TX-001", "SUCCESS", null));

            // act
            ResponseEntity<ApiResponse<PaymentV1Dto.PaymentResponse>> response = testRestTemplate.exchange(
                "/api-admin/v1/payments/" + paymentId + "/sync",
                HttpMethod.POST,
                new HttpEntity<>(null, createAdminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().status()).isEqualTo("COMPLETED")
            );

            boolean hasPaymentEvent = outboxEventJpaRepository.findByPublishedFalseOrderByCreatedAtAsc().stream()
                .anyMatch(e -> "payment-events".equals(e.getTopic()) && e.getPayload().contains("\"status\":\"COMPLETED\""));
            assertThat(hasPaymentEvent).isTrue();
        }

        @DisplayName("PG가 FAILED를 반환하면, 200 OK와 FAILED 상태를 반환하고 payment-events OutboxEvent가 저장된다.")
        @Test
        void returns200WithFailedStatus_andOutboxEventSaved_whenPgReportsFailed() {
            // arrange
            createUser();
            Product product = createProduct(10000, 10);
            Long paymentId = placeOrderAndGetPaymentId(product.getId());

            when(pgGateway.inquirePayment(any()))
                .thenReturn(new PgPaymentStatusResponse(null, "FAILED", "카드 한도 초과"));

            // act
            ResponseEntity<ApiResponse<PaymentV1Dto.PaymentResponse>> response = testRestTemplate.exchange(
                "/api-admin/v1/payments/" + paymentId + "/sync",
                HttpMethod.POST,
                new HttpEntity<>(null, createAdminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().status()).isEqualTo("FAILED")
            );

            boolean hasPaymentEvent = outboxEventJpaRepository.findByPublishedFalseOrderByCreatedAtAsc().stream()
                .anyMatch(e -> "payment-events".equals(e.getTopic()) && e.getPayload().contains("\"status\":\"FAILED\""));
            assertThat(hasPaymentEvent).isTrue();
        }

        @DisplayName("이미 COMPLETED인 결제를 sync하면, 200 OK와 변경 없이 현재 상태를 반환한다.")
        @Test
        void returns200WithPendingStatus_withoutChanges_whenPaymentAlreadyCompleted() {
            // arrange
            createUser();
            Product product = createProduct(10000, 10);
            Long paymentId = placeOrderAndGetPaymentId(product.getId());

            var payment = paymentJpaRepository.findById(paymentId).orElseThrow();
            payment.complete("TX-001");
            paymentJpaRepository.save(payment);

            // act
            ResponseEntity<ApiResponse<PaymentV1Dto.PaymentResponse>> response = testRestTemplate.exchange(
                "/api-admin/v1/payments/" + paymentId + "/sync",
                HttpMethod.POST,
                new HttpEntity<>(null, createAdminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().status()).isEqualTo("COMPLETED")
            );
        }
    }
}
