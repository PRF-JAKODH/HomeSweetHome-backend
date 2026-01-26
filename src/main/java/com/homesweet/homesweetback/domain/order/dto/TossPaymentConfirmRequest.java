package com.homesweet.homesweetback.domain.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토스페이먼츠 결제 승인 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TossPaymentConfirmRequest {

    private String paymentKey;
    private String orderId;
    private Long amount;
}
