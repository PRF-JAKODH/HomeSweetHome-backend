package com.homesweet.homesweetback.domain.product.product.domain.exception;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;

/**
 * 제품 도메인 예외
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 21.
 */
public class ProductException extends BusinessException {

    public ProductException(ErrorCode errorCode) {
        super(errorCode);
    }
}
