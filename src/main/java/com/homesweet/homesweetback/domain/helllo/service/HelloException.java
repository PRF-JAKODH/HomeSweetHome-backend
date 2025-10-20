package com.homesweet.homesweetback.domain.helllo.service;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;

/**
 * Hello 도메인 예외
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 18.
 */
public class HelloException extends BusinessException {

    public HelloException(ErrorCode errorCode) {
        super(errorCode);
    }

    public HelloException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
