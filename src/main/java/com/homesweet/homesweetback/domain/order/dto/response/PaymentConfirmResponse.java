package com.homesweet.homesweetback.domain.order.dto.response;

public record PaymentConfirmResponse(
        String merchantUid, //주문ID
        String status //최종 주문 상태
) {

}