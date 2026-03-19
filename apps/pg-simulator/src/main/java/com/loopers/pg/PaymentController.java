package com.loopers.pg;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    @Value("${pg.simulator.secret}")
    private String secret;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ConcurrentHashMap<Long, PaymentRecord> payments = new ConcurrentHashMap<>();

    private record PaymentRecord(Long paymentId, String pgTransactionId, String status, String failureReason) {}

    @PostMapping
    public Map<String, String> requestPayment(@RequestBody PaymentRequest request) {
        String pgTransactionId = UUID.randomUUID().toString();
        payments.put(request.paymentId(), new PaymentRecord(request.paymentId(), pgTransactionId, "PENDING", null));
        CompletableFuture.runAsync(() -> processPayment(request, pgTransactionId));
        return Map.of("pgTransactionId", pgTransactionId);
    }

    @GetMapping("/{paymentId}/status")
    public ResponseEntity<Map<String, Object>> getPaymentStatus(@PathVariable Long paymentId) {
        PaymentRecord record = payments.get(paymentId);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
            "paymentId", record.paymentId(),
            "pgTransactionId", record.pgTransactionId() != null ? record.pgTransactionId() : "",
            "status", record.status(),
            "failureReason", record.failureReason() != null ? record.failureReason() : ""
        ));
    }

    private void processPayment(PaymentRequest request, String pgTransactionId) {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        payments.put(request.paymentId(), new PaymentRecord(request.paymentId(), pgTransactionId, "COMPLETED", null));
        try {
            sendCallback(request, pgTransactionId);
        } catch (Exception e) {
            // 콜백 실패는 무시 (실제 PG는 재시도 처리)
        }
    }

    private void sendCallback(PaymentRequest request, String pgTransactionId) {
        String status = "COMPLETED";
        String signature = sign(request.paymentId() + "" + request.orderId() + status);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-PG-Signature", signature);

        Map<String, Object> body = Map.of(
            "orderId", request.orderId(),
            "status", status
        );

        restTemplate.postForObject(request.callbackUrl(), new HttpEntity<>(body, headers), Void.class);
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("서명 생성 실패", e);
        }
    }
}
