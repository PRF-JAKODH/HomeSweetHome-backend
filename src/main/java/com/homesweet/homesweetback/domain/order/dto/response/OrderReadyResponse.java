package com.homesweet.homesweetback.domain.order.dto.response;

// 이 정보로 프론트엔드가 토스 결제창(SDK)을 호출함.
public record OrderReadyResponse(
        String merchantUid, // 우리가 생성한 고유 주문번호
        String orderName, // 결제창에 표시될 주문 이름
        Long totalAmount // 서버가 검증한 최종 결제 금액
){
}