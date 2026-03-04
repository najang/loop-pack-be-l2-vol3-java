package com.loopers.interfaces.api;

import com.loopers.domain.user.PasswordEncoder;
import com.loopers.domain.user.UserModel;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.interfaces.api.user.UserV1Dto;
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
class UserPointApiE2ETest {

    private static final String LOGIN_ID = "testuser1";
    private static final String RAW_PASSWORD = "Test1234!";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    @DisplayName("POST /api/v1/users/me/points/charge")
    @Nested
    class ChargePoints {

        @DisplayName("양수 금액을 충전하면, 201 Created와 충전 후 잔액을 반환한다.")
        @Test
        void returns201WithUpdatedBalance_whenAmountIsPositive() {
            // arrange
            createUser(LOGIN_ID);
            UserV1Dto.ChargePointRequest request = new UserV1Dto.ChargePointRequest(10000);

            // act
            ResponseEntity<ApiResponse<UserV1Dto.ChargePointResponse>> response = testRestTemplate.exchange(
                "/api/v1/users/me/points/charge",
                HttpMethod.POST,
                new HttpEntity<>(request, createAuthHeaders(LOGIN_ID)),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data().pointBalance()).isEqualTo(10000)
            );
        }

        @DisplayName("0 이하 금액을 충전하면, 400 Bad Request를 반환한다.")
        @Test
        void returns400_whenAmountIsNotPositive() {
            // arrange
            createUser(LOGIN_ID);
            UserV1Dto.ChargePointRequest request = new UserV1Dto.ChargePointRequest(0);

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/users/me/points/charge",
                HttpMethod.POST,
                new HttpEntity<>(request, createAuthHeaders(LOGIN_ID)),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @DisplayName("인증 없이 요청하면, 401 Unauthorized를 반환한다.")
        @Test
        void returns401_whenNoAuth() {
            // arrange
            UserV1Dto.ChargePointRequest request = new UserV1Dto.ChargePointRequest(10000);

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/users/me/points/charge",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }
}
