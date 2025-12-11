<<<<<<<< HEAD:src/main/java/com/homesweet/homesweetback/domain/search/product/controller/request/ProductSortType.java
package com.homesweet.homesweetback.domain.search.product.controller.request;
========
package com.homesweet.homesweetback.domain.product.product.command.controller.request;
>>>>>>>> 9de1dca (feat: CQRS에 맞는 폴더 구조 설정):src/main/java/com/homesweet/homesweetback/domain/product/product/command/controller/request/ProductSortType.java

/**
 * 제품 조회 정렬 타입
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 23.
 */
public enum ProductSortType {
    LATEST,
    PRICE_LOW,
    PRICE_HIGH,
    POPULAR,
    RECOMMENDED
}
