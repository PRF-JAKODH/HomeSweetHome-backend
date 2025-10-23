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
    private Long stockQuantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<Long> optionValueIndexes = new ArrayList<>();

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

    public static List<Sku> createSkus(
            List<ProductCreateRequest.SkuRequest> skuRequests,
            List<ProductOptionGroup> optionGroups
    ) {
        if (skuRequests == null || skuRequests.isEmpty()) {
            return List.of();
        }

        // 전체 옵션 값 개수 (검증용)
        int totalOptionValueCount = optionGroups.stream()
                .mapToInt(group -> group.getValues().size())
                .sum();

        return skuRequests.stream()
                .map(req -> {
                    // 인덱스 검증
                    if (req.optionIndexes() != null && !req.optionIndexes().isEmpty()) {
                        for (Integer index : req.optionIndexes()) {
                            if (index == null) {
                                throw new IllegalArgumentException("옵션 인덱스에 null이 포함되어 있습니다.");
                            }
                            if (index < 0 || index >= totalOptionValueCount) {
                                throw new ProductException(ErrorCode.OUT_OF_OPTION_INDEX);
                            }
                        }
                    }

                    List<Long> optionValueIndexes = req.optionIndexes() != null
                            ? req.optionIndexes().stream()
                            .map(Integer::longValue)  // 인덱스를 Long으로 변환
                            .toList()
                            : List.of();

                    return Sku.builder()
                            .priceAdjustment(req.priceAdjustment() != null ? req.priceAdjustment() : 0)
                            .stockQuantity(req.stockQuantity())
                            .optionValueIndexes(optionValueIndexes)
                            .build();
                })
                .toList();
    }
}
