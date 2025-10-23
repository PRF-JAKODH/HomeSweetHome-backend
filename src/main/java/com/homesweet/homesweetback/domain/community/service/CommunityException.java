package com.homesweet.homesweetback.domain.community.service;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;

/**
 * Community 도메인 예외
 *
 * @author ohhalim777@gmail.com
 * @date 25. 10. 21.
 */
public class CommunityException extends BusinessException {

    public CommunityException(ErrorCode errorCode) {
        super(errorCode);
    }

    public CommunityException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
