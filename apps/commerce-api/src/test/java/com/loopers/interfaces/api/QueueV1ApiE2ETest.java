package com.loopers.interfaces.api;

import com.loopers.application.order.OutboxRelay;
import com.loopers.application.queue.WaitingQueueScheduler;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.SellingStatus;
import com.loopers.domain.queue.EntryTokenService;
import com.loopers.domain.queue.WaitingQueueService;
import com.loopers.domain.user.PasswordEncoder;
import com.loopers.domain.user.UserModel;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.pg.PgGateway;
import com.loopers.infrastructure.pg.PgPaymentResponse;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.order.OrderV1Dto;
import com.loopers.interfaces.api.queue.QueueV1Dto;
import com.loopers.support.auth.QueueEntryTokenInterceptor;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QueueV1ApiE2ETest {

    private static final String LOGIN_ID = "queueuser";
    private static final String RAW_PASSWORD = "Test1234!";

    @MockBean
    private WaitingQueueScheduler waitingQueueScheduler;

    @MockBean
    private OutboxRelay outboxRelay;

    @MockBean
    private PgGateway pgGateway;

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
    private WaitingQueueService waitingQueueService;

    @Autowired
    private EntryTokenService entryTokenService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private RedisCleanUp redisCleanUp;

    @BeforeEach
    void setUp() {
        redisCleanUp.truncateAll();
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
        redisCleanUp.truncateAll();
    }

    private UserModel createUser(String loginId) {
        return userJpaRepository.save(new UserModel(
            loginId,
            passwordEncoder.encode(RAW_PASSWORD),
            "대기열유저",
            LocalDate.of(1995, 6, 15),
            loginId + "@test.com"
        ));
    }

    private HttpHeaders authHeaders(String loginId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Loopers-LoginId", loginId);
        headers.set("X-Loopers-LoginPw", RAW_PASSWORD);
        return headers;
    }

    private Product createProduct(String name, int price, int stock, SellingStatus status) {
        Brand brand = brandJpaRepository.save(new Brand("Nike", null));
        return productJpaRepository.save(new Product(brand.getId(), name, null, price, stock, status));
    }

    @Nested
    @DisplayName("POST /api/v1/queue/enter - 대기열 진입")
    class Enter {

        @DisplayName("인증된 유저가 대기열에 진입하면 순번을 반환한다.")
        @Test
        void enter_success() {
            // arrange
            UserModel user = createUser(LOGIN_ID);
            HttpEntity<Void> request = new HttpEntity<>(authHeaders(user.getLoginId()));

            // act
            ResponseEntity<ApiResponse<QueueV1Dto.QueueStatusResponse>> response = testRestTemplate.exchange(
                "/api/v1/queue/enter",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data().position()).isEqualTo(1L),
                () -> assertThat(response.getBody().data().token()).isNull()
            );
        }

        @DisplayName("이미 대기열에 진입한 유저가 재진입해도 순번이 변경되지 않는다.")
        @Test
        void enter_duplicate_keepsSamePosition() {
            // arrange
            UserModel user = createUser(LOGIN_ID);
            HttpEntity<Void> request = new HttpEntity<>(authHeaders(user.getLoginId()));

            // act
            testRestTemplate.exchange("/api/v1/queue/enter", HttpMethod.POST, request, new ParameterizedTypeReference<ApiResponse<QueueV1Dto.QueueStatusResponse>>() {});
            ResponseEntity<ApiResponse<QueueV1Dto.QueueStatusResponse>> response = testRestTemplate.exchange(
                "/api/v1/queue/enter",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getBody().data().position()).isEqualTo(1L);
        }

        @DisplayName("인증 없이 대기열 진입 시 401을 반환한다.")
        @Test
        void enter_withoutAuth_returns401() {
            // arrange
            HttpEntity<Void> request = new HttpEntity<>(new HttpHeaders());

            // act
            ResponseEntity<ApiResponse<Void>> response = testRestTemplate.exchange(
                "/api/v1/queue/enter",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @DisplayName("동시에 10명이 진입해도 모든 순번이 유일하게 부여된다.")
        @Test
        void enter_concurrent_allPositionsUnique() throws Exception {
            // arrange: DB 커넥션 풀(테스트 환경 10개) 이내 범위로 실행
            int threadCount = 10;
            List<UserModel> users = new ArrayList<>();
            for (int i = 1; i <= threadCount; i++) {
                users.add(createUser("concuruser" + i));
            }

            List<Long> positions = new CopyOnWriteArrayList<>();
            CountDownLatch ready = new CountDownLatch(threadCount);
            CountDownLatch start = new CountDownLatch(1);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            List<Future<Boolean>> futures = users.stream()
                .map(user -> executor.submit((Callable<Boolean>) () -> {
                    HttpEntity<Void> req = new HttpEntity<>(authHeaders(user.getLoginId()));
                    ready.countDown();
                    start.await();
                    ResponseEntity<ApiResponse<QueueV1Dto.QueueStatusResponse>> res = testRestTemplate.exchange(
                        "/api/v1/queue/enter",
                        HttpMethod.POST,
                        req,
                        new ParameterizedTypeReference<>() {}
                    );
                    if (res.getStatusCode() == HttpStatus.OK && res.getBody() != null) {
                        positions.add(res.getBody().data().position());
                        return true;
                    }
                    return false;
                }))
                .toList();

            // act
            ready.await();
            start.countDown();
            executor.shutdown();
            executor.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS);

            long successCount = futures.stream().filter(f -> {
                try { return f.get(); } catch (Exception e) { return false; }
            }).count();

            // assert
            assertThat(successCount).isEqualTo(threadCount);
            assertThat(Set.copyOf(positions)).hasSize(threadCount);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/queue/position - 순번 조회")
    class GetPosition {

        @DisplayName("대기 중인 유저의 순번과 예상 대기 시간을 반환한다.")
        @Test
        void getPosition_waiting_returnsPositionAndWaitTime() {
            // arrange
            UserModel user = createUser(LOGIN_ID);
            waitingQueueService.enter(user.getId());
            HttpEntity<Void> request = new HttpEntity<>(authHeaders(user.getLoginId()));

            // act
            ResponseEntity<ApiResponse<QueueV1Dto.QueueStatusResponse>> response = testRestTemplate.exchange(
                "/api/v1/queue/position",
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().position()).isEqualTo(1L),
                () -> assertThat(response.getBody().data().estimatedWaitSeconds()).isGreaterThanOrEqualTo(0L),
                () -> assertThat(response.getBody().data().token()).isNull()
            );
        }

        @DisplayName("토큰이 발급된 유저는 position=0, token 값을 반환한다.")
        @Test
        void getPosition_tokenIssued_returnsToken() {
            // arrange
            UserModel user = createUser(LOGIN_ID);
            String token = entryTokenService.issue(user.getId());
            HttpEntity<Void> request = new HttpEntity<>(authHeaders(user.getLoginId()));

            // act
            ResponseEntity<ApiResponse<QueueV1Dto.QueueStatusResponse>> response = testRestTemplate.exchange(
                "/api/v1/queue/position",
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().position()).isEqualTo(0L),
                () -> assertThat(response.getBody().data().token()).isEqualTo(token)
            );
        }

        @DisplayName("대기열에 없고 토큰도 없는 유저는 404를 반환한다.")
        @Test
        void getPosition_notInQueue_returns404() {
            // arrange
            UserModel user = createUser(LOGIN_ID);
            HttpEntity<Void> request = new HttpEntity<>(authHeaders(user.getLoginId()));

            // act
            ResponseEntity<ApiResponse<Void>> response = testRestTemplate.exchange(
                "/api/v1/queue/position",
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("토큰 검증 - POST /api/v1/orders")
    class TokenValidation {

        @DisplayName("입장 토큰 없이 주문 API 호출 시 403을 반환한다.")
        @Test
        void createOrder_withoutToken_returns403() {
            // arrange
            UserModel user = createUser(LOGIN_ID);
            HttpHeaders headers = authHeaders(user.getLoginId());
            HttpEntity<String> request = new HttpEntity<>("{}", headers);

            // act
            ResponseEntity<ApiResponse<Void>> response = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @DisplayName("잘못된 입장 토큰으로 주문 API 호출 시 403을 반환한다.")
        @Test
        void createOrder_withInvalidToken_returns403() {
            // arrange
            UserModel user = createUser(LOGIN_ID);
            HttpHeaders headers = authHeaders(user.getLoginId());
            headers.set("X-Entry-Token", "invalid-token-value");
            HttpEntity<String> request = new HttpEntity<>("{}", headers);

            // act
            ResponseEntity<ApiResponse<Void>> response = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @DisplayName("유효한 입장 토큰으로 주문 성공 후, 동일 토큰으로 재시도하면 403을 반환한다.")
        @Test
        void createOrder_tokenConsumedAfterSuccessfulOrder_returns403() {
            // arrange
            UserModel user = createUser(LOGIN_ID);
            Product product = createProduct("에어맥스", 10000, 10, SellingStatus.SELLING);
            when(pgGateway.requestPayment(any(), any())).thenReturn(new PgPaymentResponse("TX-001"));

            String token = entryTokenService.issue(user.getId());
            HttpHeaders headers = authHeaders(user.getLoginId());
            headers.set(QueueEntryTokenInterceptor.HEADER_ENTRY_TOKEN, token);
            OrderV1Dto.CreateRequest orderRequest = new OrderV1Dto.CreateRequest(product.getId(), 1, null, "SAMSUNG", "1234-5678-9012-3456");

            // act 1 - 첫 번째 주문 (성공)
            ResponseEntity<ApiResponse<Object>> firstResponse = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(orderRequest, headers),
                new ParameterizedTypeReference<>() {}
            );
            assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

            // act 2 - 동일 토큰으로 재시도
            ResponseEntity<ApiResponse<Void>> secondResponse = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(orderRequest, headers),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }
}
