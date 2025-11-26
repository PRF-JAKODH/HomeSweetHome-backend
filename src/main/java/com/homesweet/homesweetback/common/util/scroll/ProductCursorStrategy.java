package com.homesweet.homesweetback.common.util.scroll;

import com.homesweet.homesweetback.domain.search.product.controller.request.ProductSortType;

import java.util.List;

/**
 * 상품 커서 전략 패턴
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 26.
 */
public class ProductCursorStrategy implements CursorStrategy {

    private final ProductSortType sortType;

    public ProductCursorStrategy(ProductSortType sortType) {
        this.sortType = sortType;
    }

    @Override
    public List<Object> extractSortValues(List<?> rawList) {
        return switch (sortType) {
            case RECOMMENDED -> rawList.size() >= 2
                    ? List.of(rawList.get(0), rawList.get(1)) : null;
            case POPULAR -> rawList.size() >= 4
                    ? List.of(rawList.get(0), rawList.get(1), rawList.get(2), rawList.get(3)) : null;
            case LATEST, PRICE_LOW, PRICE_HIGH -> rawList.size() >= 2
                    ? List.of(rawList.get(0), rawList.get(1)) : null;
            default -> rawList.size() >= 3
                    ? List.of(rawList.get(0), rawList.get(1), rawList.get(2)) : null;
        };
    }
}
