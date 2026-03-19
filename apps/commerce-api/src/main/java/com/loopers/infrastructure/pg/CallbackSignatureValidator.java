package com.loopers.infrastructure.pg;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Component
public class CallbackSignatureValidator {

    private final String secret;

    public CallbackSignatureValidator(@Value("${pg.simulator.secret}") String secret) {
        this.secret = secret;
    }

    public void validate(String signature, Long paymentId, Long orderId, String status) {
        String expected = sign(paymentId + "" + orderId + status);
        if (!expected.equals(signature)) {
            throw new CoreException(ErrorType.UNAUTHORIZED, "유효하지 않은 콜백 서명입니다.");
        }
    }

    public String sign(String data) {
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
