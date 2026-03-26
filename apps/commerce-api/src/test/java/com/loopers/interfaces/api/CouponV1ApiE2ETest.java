package com.loopers.interfaces.api;

import com.loopers.domain.coupon.CouponIssueResult;
import com.loopers.domain.coupon.CouponIssueStatus;
import com.loopers.domain.coupon.CouponTemplate;
import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.coupon.UserCouponStatus;
import com.loopers.domain.user.PasswordEncoder;
import com.loopers.domain.user.UserModel;
import com.loopers.infrastructure.coupon.CouponIssueResultJpaRepository;
import com.loopers.infrastructure.coupon.CouponTemplateJpaRepository;
import com.loopers.infrastructure.coupon.UserCouponJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.interfaces.api.coupon.CouponV1Dto;
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
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CouponV1ApiE2ETest {

    private static final String LOGIN_ID = "testuser1";
    private static final String RAW_PASSWORD = "Test1234!";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CouponTemplateJpaRepository couponTemplateJpaRepository;

    @Autowired
    private UserCouponJpaRepository userCouponJpaRepository;

    @Autowired
    private CouponIssueResultJpaRepository couponIssueResultJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

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

    private CouponTemplate createActiveTemplate() {
        return couponTemplateJpaRepository.save(
            new CouponTemplate("테스트 할인", CouponType.FIXED, 1000, null, ZonedDateTime.now().plusDays(7))
        );
    }

    private CouponTemplate createFcfsTemplate(int maxQuantity) {
        return couponTemplateJpaRepository.save(
            new CouponTemplate("선착순 쿠폰", CouponType.FIXED, 2000, null, ZonedDateTime.now().plusDays(7), maxQuantity)
        );
    }

    @DisplayName("POST /api/v1/coupons/{couponId}/issue")
    @Nested
    class IssueCoupon {

        @DisplayName("인증 없이 요청하면, 401 Unauthorized를 반환한다.")
        @Test
        void returns401_whenNoAuth() {
            // arrange
            CouponTemplate template = createActiveTemplate();

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/coupons/" + template.getId() + "/issue",
                HttpMethod.POST,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @DisplayName("존재하지 않는 쿠폰 ID로 요청하면, 404 Not Found를 반환한다.")
        @Test
        void returns404_whenTemplateNotFound() {
            // arrange
            createUser();

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/coupons/99999/issue",
                HttpMethod.POST,
                new HttpEntity<>(null, createAuthHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @DisplayName("비활성 쿠폰 템플릿이면, 400 Bad Request를 반환한다.")
        @Test
        void returns400_whenTemplateIsInactive() {
            // arrange
            createUser();
            CouponTemplate template = createActiveTemplate();
            template.deactivate();
            couponTemplateJpaRepository.save(template);

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/coupons/" + template.getId() + "/issue",
                HttpMethod.POST,
                new HttpEntity<>(null, createAuthHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @DisplayName("만료된 쿠폰 템플릿이면, 400 Bad Request를 반환한다.")
        @Test
        void returns400_whenTemplateIsExpired() {
            // arrange
            createUser();
            CouponTemplate template = couponTemplateJpaRepository.save(
                new CouponTemplate("만료 쿠폰", CouponType.FIXED, 1000, null, ZonedDateTime.now().minusDays(1))
            );

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/coupons/" + template.getId() + "/issue",
                HttpMethod.POST,
                new HttpEntity<>(null, createAuthHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @DisplayName("정상 요청이면, 201 Created와 발급된 쿠폰 정보를 반환한다.")
        @Test
        void returns201WithUserCouponResponse() {
            // arrange
            UserModel user = createUser();
            CouponTemplate template = createActiveTemplate();

            // act
            ResponseEntity<ApiResponse<CouponV1Dto.UserCouponResponse>> response = testRestTemplate.exchange(
                "/api/v1/coupons/" + template.getId() + "/issue",
                HttpMethod.POST,
                new HttpEntity<>(null, createAuthHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(response.getBody().data().couponTemplateId()).isEqualTo(template.getId()),
                () -> assertThat(response.getBody().data().status()).isEqualTo(UserCouponStatus.AVAILABLE.name()),
                () -> assertThat(response.getBody().data().usedAt()).isNull()
            );
        }

        @DisplayName("선착순(FCFS) 쿠폰이면, 202 Accepted와 requestId를 반환한다.")
        @Test
        void returns202WithRequestId_whenFcfsCoupon() {
            // arrange
            createUser();
            CouponTemplate fcfsTemplate = createFcfsTemplate(100);

            // act
            ResponseEntity<ApiResponse<CouponV1Dto.CouponIssueRequestResponse>> response = testRestTemplate.exchange(
                "/api/v1/coupons/" + fcfsTemplate.getId() + "/issue",
                HttpMethod.POST,
                new HttpEntity<>(null, createAuthHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED),
                () -> assertThat(response.getBody().data().requestId()).isNotNull()
            );
        }
    }

    @DisplayName("GET /api/v1/coupons/issue/{requestId}")
    @Nested
    class GetIssueResult {

        @DisplayName("인증 없이 요청하면, 401 Unauthorized를 반환한다.")
        @Test
        void returns401_whenNoAuth() {
            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/coupons/issue/some-request-id",
                HttpMethod.GET,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @DisplayName("존재하는 requestId로 조회하면, 200 OK와 발급 결과를 반환한다.")
        @Test
        void returns200WithIssueResult() {
            // arrange
            UserModel user = createUser();
            CouponTemplate fcfsTemplate = createFcfsTemplate(100);
            CouponIssueResult issueResult = couponIssueResultJpaRepository.save(
                new CouponIssueResult(user.getId(), fcfsTemplate.getId())
            );

            // act
            ResponseEntity<ApiResponse<CouponV1Dto.CouponIssueResultResponse>> response = testRestTemplate.exchange(
                "/api/v1/coupons/issue/" + issueResult.getRequestId(),
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().requestId()).isEqualTo(issueResult.getRequestId()),
                () -> assertThat(response.getBody().data().couponTemplateId()).isEqualTo(fcfsTemplate.getId()),
                () -> assertThat(response.getBody().data().status()).isEqualTo(CouponIssueStatus.PENDING.name())
            );
        }

        @DisplayName("존재하지 않는 requestId로 조회하면, 404 Not Found를 반환한다.")
        @Test
        void returns404_whenRequestIdNotFound() {
            // arrange
            createUser();

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/coupons/issue/non-existent-request-id",
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @DisplayName("GET /api/v1/users/me/coupons")
    @Nested
    class GetMyCoupons {

        @DisplayName("인증 없이 요청하면, 401 Unauthorized를 반환한다.")
        @Test
        void returns401_whenNoAuth() {
            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api/v1/users/me/coupons",
                HttpMethod.GET,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @DisplayName("쿠폰이 없으면, 빈 목록을 반환한다.")
        @Test
        void returnsEmptyList_whenNoCoupons() {
            // arrange
            createUser();

            // act
            ResponseEntity<ApiResponse<List<CouponV1Dto.UserCouponResponse>>> response = testRestTemplate.exchange(
                "/api/v1/users/me/coupons",
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data()).isEmpty()
            );
        }

        @DisplayName("쿠폰이 있으면, 내 쿠폰 목록과 상태를 반환한다.")
        @Test
        void returnsMyCoupons_withStatus() {
            // arrange
            UserModel user = createUser();
            CouponTemplate template1 = createActiveTemplate();
            CouponTemplate template2 = couponTemplateJpaRepository.save(
                new CouponTemplate("테스트 할인2", CouponType.FIXED, 2000, null, ZonedDateTime.now().plusDays(7))
            );
            userCouponJpaRepository.save(new UserCoupon(user.getId(), template1.getId()));
            UserCoupon usedCoupon = userCouponJpaRepository.save(new UserCoupon(user.getId(), template2.getId()));
            usedCoupon.use();
            userCouponJpaRepository.save(usedCoupon);

            // act
            ResponseEntity<ApiResponse<List<CouponV1Dto.UserCouponResponse>>> response = testRestTemplate.exchange(
                "/api/v1/users/me/coupons",
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data()).hasSize(2)
            );
        }
    }
}
