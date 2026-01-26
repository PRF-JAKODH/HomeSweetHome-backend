package com.homesweet.homesweetback.domain.order.dto;

import com.homesweet.homesweetback.domain.order.entity.Payment;
import com.homesweet.homesweetback.domain.order.entity.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 결제 응답 DTO
 */
@Getter
@Builder
public class PaymentResponse {

    private Long paymentId;
    private Long orderId;
    private String orderNumber;

    /**
     * 토스페이먼츠 paymentKey
     */
    private String paymentKey;

    private PaymentStatus status;
    private Long amount;

    /**
     * 결제 수단 (카드, 가상계좌, 간편결제 등)
     */
    private String method;

    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private String receiptUrl;

    public static PaymentResponse from(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrder().getId())
                .orderNumber(payment.getTossOrderId())
                .paymentKey(payment.getPaymentKey())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .method(payment.getMethod())
                .requestedAt(payment.getRequestedAt())
                .approvedAt(payment.getApprovedAt())
                .receiptUrl(payment.getReceiptUrl())
                .build();
    }
}
