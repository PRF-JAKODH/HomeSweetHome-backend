package com.homesweet.homesweetback.domain.product.product.domain;

import com.homesweet.homesweetback.domain.product.product.controller.request.ProductCreateRequest;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 옵션 그룹 도메인
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 22.
 */
@Getter
@Builder
public class ProductOptionGroup {

    private Long id;
    private Long productId;
    private String groupName;
    private LocalDateTime createdAt;

    @Builder.Default
    private List<ProductOptionValue> values = new ArrayList<>();
}
