package com.homesweet.homesweetback.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 토스페이먼츠 API 설정
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "payments.toss")
public class TossPaymentsConfig {

    private String secretKey;

    private static final String BASE_URL = "https://api.tosspayments.com/v1";

    public String getConfirmUrl() {
        return BASE_URL + "/payments/confirm";
    }

    public String getCancelUrl(String paymentKey) {
        return BASE_URL + "/payments/" + paymentKey + "/cancel";
    }

    public String getPaymentUrl(String paymentKey) {
        return BASE_URL + "/payments/" + paymentKey;
    }

    public String getOrderIdUrl(String orderId) {
        return BASE_URL + "/payments/orders/" + orderId;
    }
}
