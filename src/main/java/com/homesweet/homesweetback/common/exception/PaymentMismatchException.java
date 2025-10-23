package com.homesweet.homesweetback.common.exception;

// 결제
// 400 Bad Request
// (OrderServiceTest의 185줄에서 이 클래스를 사용함)
public class PaymentMismatchException extends RuntimeException {
    public PaymentMismatchException(String message) {
        super(message);
    }
}
// 예외 상황
// OrderService의 confirmPayment(API 2) 메서드에서 2가지 상황에 발생함.
// 상황 A: 금액 위변조
// DB에 저장된 Order.totalAmount(50,000원)와
// 프론트엔드가 보낸 PaymentConfirmRequest.amount(30,000원)가
// 일치하지 않을 때, Service는 금액 위변조로 판단하고 이 예외를 throw함.

// GlobalExceptionHandler가 이 예외를 잡아채면, 우리가 정의한 '400' 응답을 프론트엔드에 보여줌.
// 결제 금액이 일치하지 않습니다라는 명확한 JSON을 프론트엔드에 반환함.