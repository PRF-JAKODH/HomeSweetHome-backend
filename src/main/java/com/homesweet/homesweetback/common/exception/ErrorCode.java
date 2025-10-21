package com.homesweet.homesweetback.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드 모음
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 18.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 에러입니다."),

    // Product
    DUPLICATED_CATEGORY_NAME_ERROR(HttpStatus.CONFLICT, "이미 해당하는 카테고리 이름이 존재합니다"),
    CANNOT_FOUND_PARENT_CATEGORY_ERROR(HttpStatus.NOT_FOUND, "부모 카테고리를 찾을 수 없습니다"),
    CATEGORY_DEPTH_EXCEEDED_ERROR(HttpStatus.BAD_REQUEST, "카테고리 기준 깊이를 넘었습니다");

    private final HttpStatus status;
    private final String message;
}
