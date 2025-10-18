package com.homesweet.homesweetback.common.exception;

import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;

/**
 * 공통 예외 응답 DTO
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 18.
 */
public record ErrorResponse(
        int status,
        String message,
        String timestamp
) {
    public static ErrorResponse of(HttpStatus status, String message) {
        return new ErrorResponse(
                status.value(),
                message,
                OffsetDateTime.now().toString()
        );
    }
}
