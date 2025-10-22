package com.homesweet.homesweetback.domain.product.product.domain;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.product.product.controller.request.ProductCreateRequest;
import com.homesweet.homesweetback.domain.product.product.domain.exception.ProductException;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SKU 도메인
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 22.
 */
@Getter
@Builder
public class Sku {

    private Long id;
    private Long productId;
    private Integer priceAdjustment;
    private Integer stockQuantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<Long> optionValueIds = new ArrayList<>();

    /**
     * 재고 차감
     */
    public void decreaseStock(int quantity) {
        if (stockQuantity < quantity) {
            throw new ProductException(ErrorCode.OUT_OF_STOCK);
        }
        this.stockQuantity -= quantity;
    }

    /**
     * 재고 증가
     */
    public void increaseStock(int quantity) {
        this.stockQuantity += quantity;
    }

    /**
     * 재고 여부 확인
     */
    public boolean isInStock() {
        return stockQuantity > 0;
    }
}
