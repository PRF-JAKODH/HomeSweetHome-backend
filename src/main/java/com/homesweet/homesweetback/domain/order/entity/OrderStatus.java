package com.homesweet.homesweetback.domain.order.entity;

public enum OrderStatus {
    PENDING, // 결제 대기
    COMPLETED, // 결제 완료
    FAILED, // 결제 실패
}