package com.homesweet.homesweetback.domain.order.entity;

public enum OrderStatus {
    PENDING,  // 결제 대기
    DELIVERED, // 배송 완료 (우리는 결제 완료 시 바로 이 상태로 변경)
    CANCELED  // 주문 취소
}