package com.homesweet.homesweetback.domain.order.entity;

public enum DeliveryStatus {
    READY, // 배송 준비
    DELIVERING, // 배송 중
    DELIVERED, // 배송 완료
    CANCELLED, // 주문 취소 = 환불
}
