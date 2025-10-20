package com.homesweet.homesweetback.domain.product.dto.type;

import lombok.Getter;

/**
 * 판매 제품 상태 관리
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 20.
 */
@Getter
public enum ProductStatus {
    ON_SALE("판매중"),
    OUT_OF_STOCK("품절"),
    SUSPENDED("판매중지");

    private String label;

    ProductStatus(String label) {
        this.label = label;
    }
}
