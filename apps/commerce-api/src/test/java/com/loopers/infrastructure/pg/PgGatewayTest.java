package com.loopers.infrastructure.pg;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PgGatewayTest {

    @Mock
    private PgSimulatorClient client;

    @InjectMocks
    private PgGateway pgGateway;

    @DisplayName("inquirePayment 호출 시,")
    @Nested
    class InquirePayment {

        @DisplayName("클라이언트가 성공하면 상태 응답을 반환한다.")
        @Test
        void returnsStatusResponse_whenClientSucceeds() {
            // arrange
            PgPaymentStatusResponse expected = new PgPaymentStatusResponse("TX-001", "SUCCESS", null);
            when(client.inquirePayment("TX-001")).thenReturn(expected);

            // act
            PgPaymentStatusResponse result = pgGateway.inquirePayment("TX-001");

            // assert
            assertThat(result).isEqualTo(expected);
        }

        @DisplayName("클라이언트가 예외를 던지면, 예외가 전파된다.")
        @Test
        void throwsPaymentFailed_whenClientThrows() {
            // arrange
            when(client.inquirePayment("TX-001")).thenThrow(new RuntimeException("timeout"));

            // act & assert
            // 단위 테스트에서는 Spring AOP가 동작하지 않으므로 예외가 직접 전파됨
            // 실제 Circuit Breaker fallback 동작은 E2E 테스트에서 검증
            assertThrows(RuntimeException.class, () -> pgGateway.inquirePayment("TX-001"));
        }
    }
}
