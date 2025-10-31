package com.homesweet.homesweetback.domain.community.dto.exception;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;

public class CommunityException extends BusinessException {

    public CommunityException(ErrorCode errorCode) {
        super(errorCode);
    }

    public CommunityException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
