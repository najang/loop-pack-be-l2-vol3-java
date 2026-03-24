package com.loopers.interfaces.api;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.SellingStatus;
import com.loopers.domain.user.PasswordEncoder;
import com.loopers.domain.user.UserModel;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.order.OrderJpaRepository;
import com.loopers.infrastructure.payment.PaymentJpaRepository;
import com.loopers.infrastructure.pg.PgGateway;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.interfaces.api.order.OrderV1Dto;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
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
class OrderCreateWithPgFailureE2ETest {

    private static final String LOGIN_ID = "testpgfailure1";
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
    private DatabaseCleanUp databaseCleanUp;

    @MockBean
    private PgGateway pgGateway;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
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

    private Product createProduct(int price, int stock) {
        Brand brand = brandJpaRepository.save(new Brand("Nike", null));
        return productJpaRepository.save(new Product(brand.getId(), "에어맥스", null, price, stock, SellingStatus.SELLING));
    }

    @DisplayName("PG 요청이 실패하면, Payment가 FAILED 상태로 저장되어 복구 불가 PENDING이 남지 않는다.")
    @Test
    void paymentIsMarkedFailed_whenPgRequestThrowsException() {
        // arrange
        createUser();
        Product product = createProduct(10000, 10);
        when(pgGateway.requestPayment(any(), any())).thenThrow(new RuntimeException("PG 서버 오류"));

        OrderV1Dto.CreateRequest request = new OrderV1Dto.CreateRequest(product.getId(), 1, null, CARD_TYPE, CARD_NO);

        // act
        ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
            "/api/v1/orders",
            HttpMethod.POST,
            new HttpEntity<>(request, createAuthHeaders()),
            new ParameterizedTypeReference<>() {}
        );

        // assert
        assertAll(
            () -> assertThat(response.getStatusCode().is5xxServerError()).isTrue(),
            () -> assertThat(paymentJpaRepository.findAll())
                .isNotEmpty()
                .allMatch(payment -> payment.getStatus() == PaymentStatus.FAILED),
            () -> assertThat(paymentJpaRepository.findAll())
                .noneMatch(payment -> payment.getStatus() == PaymentStatus.PENDING)
        );
    }
}
