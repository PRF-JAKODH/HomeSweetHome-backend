package com.homesweet.homesweetback.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * 공통 예외 처리
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 18.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 비지니스 예외
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        log.error("[Business Exception]: {}", ex.getMessage(), ex);
        ErrorCode errorCode = ex.getErrorCode();
        ErrorResponse errorResponse = ErrorResponse.of(errorCode.getStatus(), errorCode.getMessage());

        return ResponseEntity.status(errorCode.getStatus()).body(errorResponse);
    }

    // 그 외 모든 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("[Exception]: {}", ex.getMessage(), ex);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        ErrorResponse errorResponse = ErrorResponse.of(errorCode.getStatus(), errorCode.getMessage());

        return ResponseEntity.status(errorCode.getStatus()).body(errorResponse);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPart(MissingServletRequestPartException ex) {
        ErrorCode errorCode = ErrorCode.MISSING_INPUT_DATA;
        ErrorResponse errorResponse = ErrorResponse.of(errorCode.getStatus(), errorCode.getMessage());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(errorResponse);
    }
}
