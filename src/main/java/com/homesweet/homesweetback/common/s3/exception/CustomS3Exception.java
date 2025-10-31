package com.homesweet.homesweetback.common.s3.exception;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;

/**
 * S3 관련 예외 처리
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 22.
 */
public class CustomS3Exception extends BusinessException {
    public CustomS3Exception(ErrorCode errorCode) {
        super(errorCode);
    }

    public CustomS3Exception(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
