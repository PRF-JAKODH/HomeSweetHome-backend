package com.homesweet.homesweetback.domain.product.product.domain;

import lombok.Getter;

/**
 * 판매 제품 상태 관리
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 20.
 */
@Getter
public enum ProductStatus {
    ON_SALE,
    OUT_OF_STOCK,
    SUSPENDED;
}
