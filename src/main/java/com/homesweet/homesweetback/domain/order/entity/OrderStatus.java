package com.homesweet.homesweetback.domain.order.entity;

/**
 * 주문 상태
 */
public enum OrderStatus {
    PENDING,    // 결제 대기
    PAID,       // 결제 완료
    CANCELLED   // 주문 취소
}
