package com.homesweet.homesweetback.domain.order.dto.internal;

import java.time.LocalDateTime;

public record PendingPayment(
        String orderNumber,       // 주문을 찾기 위한 키
        String pgTransactionId,   // paymentKey
        long amount,
        String method,
        String paymentStatus,
        LocalDateTime paidAt,
        String pgRawData          // JSON 문자열
) {}
