package com.homesweet.homesweetback.domain.product.product.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 옵션 값 도메인
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 22.
 */
@Getter
@Builder
public class ProductOptionValue {

    private Long id;
    private Long groupId;
    private String value;
    private LocalDateTime createdAt;
}
