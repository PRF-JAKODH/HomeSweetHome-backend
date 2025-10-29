//프론트엔드에서 토스 결제창을 성공적으로 띄우면, 토스로부터 리다이렉트되면서 쿼리 파라미터로 받는 값들.
//프론트는 이 값들을 받아서 즉시 백엔드(이 API)로 쏴줘야 함.
package com.homesweet.homesweetback.domain.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentConfirmRequest(
        @NotBlank(message = "토스페이먼츠 paymentKey가 비어있습니다.")
        String paymentKey,

        @NotBlank(message = "주문 ID(merchant_uid)가 비어있습니다.")
        Long orderId,

        @NotNull(message = "결제 금액이 비어있습니다.")
        Long amount
){
}