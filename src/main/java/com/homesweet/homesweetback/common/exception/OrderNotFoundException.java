package com.homesweet.homesweetback.common.exception;

// 주문
// 404 Not Found 응답을 위한 예외
// (OrderServiceTest의 163줄에서 이 클래스를 사용함)
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String message) {
        super(message);
    }
}
// OrderSerivce의 confirmPayment(API 2) 메서드에서 발생함.
// 1. 프론트엔드에서 결제 검증 요청(PaymentConfirmRequest)이 들어옴.
// 2. Service는 요청에 담긴 orderId(merchant_uid)를 가지고 orderRepository.findByMerchantUid를 호출해 DB에서 주문을 찾음.
// 3. 이때, 해커의 공격이나 버그로 인해 orderId가 non-existent-uid처럼 DB에 존재하지 않는 값일 수 있음.
// 4. findByMerchantUid는 Optional.empty() (빈 박스)를 반환함.
// 5. OrderService 92줄의 .orElseThrow(...)가 실행되면서 new OrderNotFoundException(...)을 throw함

// RuntimeException은 에러 구분없이 500 Internal Server Error(서버 고장)로 처리 됨
// 이 예외 처리는 주문을 못 찾은 상황이기 때문에 클라이언트가 존재하지 않는 리소스를 요청한 404 Not Found에러임.