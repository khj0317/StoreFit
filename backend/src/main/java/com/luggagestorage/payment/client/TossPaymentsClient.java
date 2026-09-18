package com.luggagestorage.payment.client;

import com.luggagestorage.common.exception.BusinessException;
import com.luggagestorage.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component
public class TossPaymentsClient {

    private final RestClient restClient;

    public TossPaymentsClient(@Value("${toss.secret-key}") String secretKey) {
        String encodedAuth = Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        this.restClient = RestClient.builder()
            .baseUrl("https://api.tosspayments.com")
            .defaultHeader("Authorization", "Basic " + encodedAuth)
            .build();
    }

    public TossConfirmResult confirm(String paymentKey, String orderId, Integer amount) {
        try {
            return restClient.post()
                .uri("/v1/payments/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("paymentKey", paymentKey, "orderId", orderId, "amount", amount))
                .retrieve()
                .body(TossConfirmResult.class);
        } catch (RestClientResponseException e) {
            throw new BusinessException(ErrorCode.PAYMENT_CONFIRM_FAILED);
        }
    }
}
