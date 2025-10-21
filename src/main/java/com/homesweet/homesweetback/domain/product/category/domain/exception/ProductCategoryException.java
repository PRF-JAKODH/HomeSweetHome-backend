package com.homesweet.homesweetback.domain.product.category.domain.exception;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;

/**
 * 제품 카테고리 도메인 예외 클래스
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 21.
 */
public class ProductCategoryException extends BusinessException {

    public ProductCategoryException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ProductCategoryException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
