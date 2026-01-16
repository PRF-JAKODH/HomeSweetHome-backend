package com.homesweet.homesweetback.domain.order.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 주문 생성 요청 DTO
 * 장바구니에서 선택한 상품들로 주문을 생성할 때 사용
 */
@Getter
@NoArgsConstructor
public class CreateOrderRequest {

    /**
     * 장바구니 ID 목록
     */
    @NotEmpty(message = "최소 하나 이상의 장바구니 항목이 필요합니다.")
    private List<Long> cartIds;

    public CreateOrderRequest(List<Long> cartIds) {
        this.cartIds = cartIds;
    }
}
