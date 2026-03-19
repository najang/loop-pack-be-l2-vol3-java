package com.loopers.interfaces.api.payment;

import com.loopers.application.payment.PaymentFacade;
import com.loopers.infrastructure.pg.CallbackSignatureValidator;
import com.loopers.infrastructure.pg.PgCallbackRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentCallbackV1Controller {

    private final PaymentFacade paymentFacade;
    private final CallbackSignatureValidator signatureValidator;

    @PostMapping("/{paymentId}/callback")
    @ResponseStatus(HttpStatus.OK)
    public void handleCallback(
        @PathVariable Long paymentId,
        @RequestHeader("X-PG-Signature") String signature,
        @Valid @RequestBody PaymentV1Dto.CallbackRequest request
    ) {
        signatureValidator.validate(signature, paymentId, request.orderId(), request.status());

        PgCallbackRequest callback = new PgCallbackRequest(
            paymentId,
            request.orderId(),
            request.status(),
            request.failureReason()
        );
        paymentFacade.handleCallback(callback);
    }
}
