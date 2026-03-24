package com.loopers.interfaces.api.payment;

import com.loopers.application.payment.PaymentFacade;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api-admin/v1/payments")
public class PaymentAdminV1Controller {

    private final PaymentFacade paymentFacade;

    @PostMapping("/{paymentId}/sync")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<PaymentV1Dto.PaymentResponse> syncPayment(@PathVariable Long paymentId) {
        return ApiResponse.success(PaymentV1Dto.PaymentResponse.from(
            paymentFacade.syncWithPg(paymentId)));
    }
}
