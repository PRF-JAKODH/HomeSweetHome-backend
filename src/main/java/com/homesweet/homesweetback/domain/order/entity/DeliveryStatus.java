package com.homesweet.homesweetback.domain.order.entity;

public enum DeliveryStatus {
    BEFORE_SHIPMENT,  // 배송 전
    DELIVERING, // 배송 중
    DELIVERED, // 배송 완료
    CANCELLED, // 주문 취소 = 환불
}
