package com.homesweet.homesweetback.domain.order.entity;

/**
 * 결제 상태 (토스페이먼츠 기준)
 */
public enum PaymentStatus {
    READY, // 결제 준비
    DONE, // 결제 완료
    CANCELLED // 결제 취소
}
