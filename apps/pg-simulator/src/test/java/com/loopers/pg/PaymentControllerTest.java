package com.loopers.pg;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"pg.simulator.secret=test-secret"})
@AutoConfigureMockMvc
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @DisplayName("GET /api/v1/payments/{paymentId}/status")
    @Nested
    class GetPaymentStatus {

        @DisplayName("결제 요청 후 처리가 완료되면, 200 OK와 COMPLETED 상태를 반환한다.")
        @Test
        void returns200WithCompletedStatus_afterPaymentProcessed() throws Exception {
            // arrange
            String requestBody = """
                {
                    "paymentId": 1001,
                    "orderId": 2001,
                    "cardType": "SAMSUNG",
                    "cardNo": "1234-5678-9012-3456",
                    "amount": 10000,
                    "callbackUrl": "http://localhost:9999/dummy"
                }
                """;

            mockMvc.perform(post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

            Thread.sleep(500);

            // act & assert
            mockMvc.perform(get("/api/v1/payments/1001/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @DisplayName("존재하지 않는 결제 ID로 조회하면, 404 Not Found를 반환한다.")
        @Test
        void returns404_whenPaymentIdNotFound() throws Exception {
            // act & assert
            mockMvc.perform(get("/api/v1/payments/99999/status"))
                .andExpect(status().isNotFound());
        }
    }
}
