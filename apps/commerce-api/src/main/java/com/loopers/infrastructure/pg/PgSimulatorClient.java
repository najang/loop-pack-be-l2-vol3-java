package com.loopers.infrastructure.pg;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "pg-simulator", url = "${pg.simulator.url}")
public interface PgSimulatorClient {

    @PostMapping("/api/v1/payments")
    PgPaymentResponse requestPayment(@RequestBody PgPaymentRequest request);

    @GetMapping("/api/v1/payments/{paymentId}/status")
    PgPaymentStatusResponse inquirePayment(@PathVariable("paymentId") Long paymentId);
}
