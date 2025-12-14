package com.homesweet.homesweetback.domain.order.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 토스페이먼츠 결제 승인 요청 DTO
 */
@Getter
@Builder
public class TossPaymentConfirmRequest {

    private String paymentKey;
    private String orderId;
    private Long amount;
}
