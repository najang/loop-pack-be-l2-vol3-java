package com.loopers.interfaces.api;

import com.loopers.domain.eventlog.EventLog;
import com.loopers.infrastructure.eventlog.EventLogJpaRepository;
import com.loopers.interfaces.api.eventlog.EventLogAdminV1Dto;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EventLogAdminV1ApiE2ETest {

    private static final String ADMIN_HEADER_VALUE = "loopers.admin";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private EventLogJpaRepository eventLogJpaRepository;

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

    @DisplayName("GET /api-admin/v1/event-logs")
    @Nested
    class GetEventLogs {

        @DisplayName("어드민 인증 없이 요청하면, 401 Unauthorized를 반환한다.")
        @Test
        void returns401_whenNoAdminAuth() {
            // act
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                "/api-admin/v1/event-logs",
                HttpMethod.GET,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @DisplayName("이벤트 로그가 있으면, 200 OK와 페이징된 목록을 반환한다.")
        @Test
        void returns200WithPaginatedEventLogs() {
            // arrange
            eventLogJpaRepository.save(new EventLog("PRODUCT_VIEWED", 1L, 100L, null));
            eventLogJpaRepository.save(new EventLog("LIKED", 1L, 200L, null));
            eventLogJpaRepository.save(new EventLog("ORDER_CREATED", 2L, 300L, "{\"productId\":100}"));

            // act
            ResponseEntity<ApiResponse<EventLogAdminV1Dto.EventLogPageResponse>> response = testRestTemplate.exchange(
                "/api-admin/v1/event-logs?page=0&size=20",
                HttpMethod.GET,
                new HttpEntity<>(createAdminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().totalElements()).isEqualTo(3)
            );
        }
    }
}
