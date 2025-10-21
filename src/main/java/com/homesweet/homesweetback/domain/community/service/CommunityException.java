package com.homesweet.homesweetback.domain.community.service;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;

/**
 * Hello 도메인 예외
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 18.
 */
public class CommunityException extends BusinessException {

    public CommunityException(ErrorCode errorCode) {
        super(errorCode);
    }

    public CommunityException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
