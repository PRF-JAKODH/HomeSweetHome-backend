package com.homesweet.homesweetback.domain.order.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 토스페이먼츠 결제 취소 요청 DTO
 */
@Getter
@Builder
public class TossPaymentCancelRequest {

    private String cancelReason;
    private Long cancelAmount;
}
