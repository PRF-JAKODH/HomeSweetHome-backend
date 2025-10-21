package com.homesweet.homesweetback.domain.order.dto.request;//이 코드가 속한 패키지를 선언함

import jakarta.validation.Valid; // jakarta.validation 패키지에서 @Valid 어노테이션을 가져옴.
import jakarta.validation.constraints.NotBlank; //NotBlank 유효성 검사 어노테이션 가져오기
import jakarta.validation.constraints.NotEmpty; //NotEmpty 유효성 검사 어노테이션 가져오기
import jakarta.validation.constraints.NotNull; //Not Null 유효성 검사 어노테이션 가져오기
import jakarta.validation.constraints.Min; //Min 유효성 검사 어노테이션 가져오기
import java.util.List; //자바 표준 라이브러리. java.util패키지에서 List인터페이스 가져오기

public record CreateOrderRequest(

        @Valid //리스트 안의 객체(OrderItemRequst)도 유효성 검사를 하도록 함
        @NotEmpty(message = "주문 상품 목록이 비어있습니다.")//orderItem은 리스트이기 때문에 NotEmpty
        List<OrderItemRequest> orderItems,

        @NotBlank(message = "배송지 주소가 비어있습니다")
        String shippingAddress,

        @NotBlank(message = "수령인 이름이 비어있습니다.")
        String recipientName,

        @NotBlank(message = "수령인 연락처가 비어있습니다.")
        String recipientPhone
){
    public record OrderItemRequest(
            @NotNull(message = "상품 ID가 필요합니다.")
            Long productId,

            @Min(value = 1, message = "주문 수량은 1개 이상이어야 합니다.")
            int quantity
    ){}
}