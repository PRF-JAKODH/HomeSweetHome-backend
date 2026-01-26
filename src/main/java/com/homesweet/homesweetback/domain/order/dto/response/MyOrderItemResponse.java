package com.homesweet.homesweetback.domain.order.dto.response;

import lombok.Builder;

@Builder
public record MyOrderItemResponse (
    Long orderId, // key로 사용, '주문 상세'/'주문 취소' 버튼 클릭 시 ID 전달, DB가 사용하는 '내부 관리용 ID'
    String orderNumber, // 주문 상세 모달에서 주문번호 표시, 토스 페이먼츠와 통신할 때 사용하는 ID
    String orderDate, // 주문일 표시(2025-10-10)

    String productName, // 상품 이름 표시
    String imageUrl, // 상품 이미지 표시
    Long price, // 상품 가격("890,000원"), 대표 상품의 단가

    String orderStatus, // 상대 텍스트의 색상 구분
    String deliveryStatus // '배송 완료', '배송 중'등 상태 텍스트 표시
){
}
