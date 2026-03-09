package com.loopers.interfaces.api;

import com.loopers.domain.coupon.CouponTemplate;
import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.coupon.UserCoupon;
import com.loopers.infrastructure.coupon.CouponTemplateJpaRepository;
import com.loopers.infrastructure.coupon.UserCouponJpaRepository;
import com.loopers.interfaces.api.coupon.CouponAdminV1Dto;
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

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CouponAdminV1ApiE2ETest {

    private static final String ADMIN_HEADER_VALUE = "loopers.admin";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private CouponTemplateJpaRepository couponTemplateJpaRepository;

    @Autowired
    private UserCouponJpaRepository userCouponJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private HttpHeaders createAdminHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Loopers-Ldap", ADMIN_HEADER_VALUE);
        return headers;
    }

    private CouponAdminV1Dto.CreateRequest buildCreateRequest(String name, CouponType type, int value) {
        return new CouponAdminV1Dto.CreateRequest(name, type, value, null, ZonedDateTime.now().plusDays(30));
    }

    private CouponTemplate saveTemplate(String name) {
        return couponTemplateJpaRepository.save(
            new CouponTemplate(name, CouponType.FIXED, 1000, null, ZonedDateTime.now().plusDays(7))
        );
    }

    @DisplayName("GET /api-admin/v1/coupons")
    @Nested
    class GetCoupons {

        @DisplayName("어드민 인증 없이 요청하면, 401 Unauthorized를 반환한다.")
        @Test
        void returns401_whenNoAdminAuth() {
            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api-admin/v1/coupons",
                HttpMethod.GET,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @DisplayName("쿠폰 목록을 페이징으로 조회하면, 200 OK와 목록을 반환한다.")
        @Test
        void returns200WithCouponList() {
            // arrange
            saveTemplate("쿠폰A");
            saveTemplate("쿠폰B");

            // act
            ResponseEntity<ApiResponse<CouponAdminV1Dto.CouponPageResponse>> response = testRestTemplate.exchange(
                "/api-admin/v1/coupons?page=0&size=20",
                HttpMethod.GET,
                new HttpEntity<>(createAdminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().totalElements()).isEqualTo(2)
            );
        }
    }

    @DisplayName("GET /api-admin/v1/coupons/{couponId}")
    @Nested
    class GetCoupon {

        @DisplayName("존재하는 쿠폰 ID로 조회하면, 200 OK와 상세 정보를 반환한다.")
        @Test
        void returns200WithCouponDetail() {
            // arrange
            CouponTemplate template = saveTemplate("상세 쿠폰");

            // act
            ResponseEntity<ApiResponse<CouponAdminV1Dto.CouponResponse>> response = testRestTemplate.exchange(
                "/api-admin/v1/coupons/" + template.getId(),
                HttpMethod.GET,
                new HttpEntity<>(createAdminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().name()).isEqualTo("상세 쿠폰"),
                () -> assertThat(response.getBody().data().isActive()).isTrue()
            );
        }

        @DisplayName("존재하지 않는 쿠폰 ID로 조회하면, 404 Not Found를 반환한다.")
        @Test
        void returns404_whenNotFound() {
            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api-admin/v1/coupons/99999",
                HttpMethod.GET,
                new HttpEntity<>(createAdminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @DisplayName("POST /api-admin/v1/coupons")
    @Nested
    class CreateCoupon {

        @DisplayName("정상 요청이면, 201 Created와 생성된 쿠폰 템플릿을 반환한다.")
        @Test
        void returns201WithCreatedTemplate() {
            // arrange
            CouponAdminV1Dto.CreateRequest request = buildCreateRequest("신규 쿠폰", CouponType.FIXED, 5000);

            // act
            ResponseEntity<ApiResponse<CouponAdminV1Dto.CouponResponse>> response = testRestTemplate.exchange(
                "/api-admin/v1/coupons",
                HttpMethod.POST,
                new HttpEntity<>(request, createAdminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(response.getBody().data().id()).isNotNull(),
                () -> assertThat(response.getBody().data().name()).isEqualTo("신규 쿠폰"),
                () -> assertThat(response.getBody().data().type()).isEqualTo(CouponType.FIXED.name()),
                () -> assertThat(response.getBody().data().value()).isEqualTo(5000),
                () -> assertThat(response.getBody().data().isActive()).isTrue()
            );
        }

        @DisplayName("필수 필드 누락 시, 400 Bad Request를 반환한다.")
        @Test
        void returns400_whenRequiredFieldMissing() {
            // arrange - name is null
            CouponAdminV1Dto.CreateRequest request = new CouponAdminV1Dto.CreateRequest(
                null, CouponType.FIXED, 1000, null, ZonedDateTime.now().plusDays(7)
            );

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api-admin/v1/coupons",
                HttpMethod.POST,
                new HttpEntity<>(request, createAdminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @DisplayName("PUT /api-admin/v1/coupons/{couponId}")
    @Nested
    class UpdateCoupon {

        @DisplayName("정상 요청이면, 200 OK와 수정된 쿠폰 템플릿을 반환한다.")
        @Test
        void returns200WithUpdatedTemplate() {
            // arrange
            CouponTemplate template = saveTemplate("원래 쿠폰");
            CouponAdminV1Dto.UpdateRequest request = new CouponAdminV1Dto.UpdateRequest(
                "수정된 쿠폰", CouponType.RATE, 20, 10000, ZonedDateTime.now().plusDays(60)
            );

            // act
            ResponseEntity<ApiResponse<CouponAdminV1Dto.CouponResponse>> response = testRestTemplate.exchange(
                "/api-admin/v1/coupons/" + template.getId(),
                HttpMethod.PUT,
                new HttpEntity<>(request, createAdminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().name()).isEqualTo("수정된 쿠폰"),
                () -> assertThat(response.getBody().data().type()).isEqualTo(CouponType.RATE.name()),
                () -> assertThat(response.getBody().data().value()).isEqualTo(20),
                () -> assertThat(response.getBody().data().minOrderAmount()).isEqualTo(10000)
            );
        }

        @DisplayName("존재하지 않는 쿠폰 ID로 수정하면, 404 Not Found를 반환한다.")
        @Test
        void returns404_whenNotFound() {
            // arrange
            CouponAdminV1Dto.UpdateRequest request = new CouponAdminV1Dto.UpdateRequest(
                "수정", CouponType.FIXED, 1000, null, ZonedDateTime.now().plusDays(7)
            );

            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api-admin/v1/coupons/99999",
                HttpMethod.PUT,
                new HttpEntity<>(request, createAdminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @DisplayName("DELETE /api-admin/v1/coupons/{couponId}")
    @Nested
    class DeleteCoupon {

        @DisplayName("정상 요청이면, 204 No Content를 반환하고 soft delete 처리된다.")
        @Test
        void returns204AndSoftDeletes() {
            // arrange
            CouponTemplate template = saveTemplate("삭제 쿠폰");

            // act
            ResponseEntity<Void> response = testRestTemplate.exchange(
                "/api-admin/v1/coupons/" + template.getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(createAdminHeaders()),
                Void.class
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

            CouponTemplate deleted = couponTemplateJpaRepository.findById(template.getId()).orElseThrow();
            assertThat(deleted.getDeletedAt()).isNotNull();
        }

        @DisplayName("존재하지 않는 쿠폰 ID로 삭제하면, 404 Not Found를 반환한다.")
        @Test
        void returns404_whenNotFound() {
            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api-admin/v1/coupons/99999",
                HttpMethod.DELETE,
                new HttpEntity<>(createAdminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @DisplayName("GET /api-admin/v1/coupons/{couponId}/issues")
    @Nested
    class GetIssuedCoupons {

        @DisplayName("발급 내역이 있으면, 200 OK와 발급 목록을 반환한다.")
        @Test
        void returns200WithIssuedCoupons() {
            // arrange
            CouponTemplate template = saveTemplate("발급 쿠폰");
            userCouponJpaRepository.save(new UserCoupon(1L, template.getId()));
            userCouponJpaRepository.save(new UserCoupon(2L, template.getId()));

            // act
            ResponseEntity<ApiResponse<CouponAdminV1Dto.UserCouponPageResponse>> response = testRestTemplate.exchange(
                "/api-admin/v1/coupons/" + template.getId() + "/issues?page=0&size=20",
                HttpMethod.GET,
                new HttpEntity<>(createAdminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().totalElements()).isEqualTo(2)
            );
        }

        @DisplayName("존재하지 않는 쿠폰 ID로 발급 내역 조회하면, 404 Not Found를 반환한다.")
        @Test
        void returns404_whenCouponNotFound() {
            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api-admin/v1/coupons/99999/issues",
                HttpMethod.GET,
                new HttpEntity<>(createAdminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
