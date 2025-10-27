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
    // System
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 에러입니다"),
    FILE_STREAM_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "파일 스트림 처리 중 오류가 발생했습니다"),
    MISSING_INPUT_DATA(HttpStatus.BAD_REQUEST, "필수적인 입력값을 전달받지 못했습니다"),

    // S3
    FAILED_UPLOAD_S3_ERROR(HttpStatus.BAD_REQUEST, "S3 저장소에 업로드를 실패했습니다"),
    INVALID_FILE_ERROR(HttpStatus.BAD_REQUEST, "유효하지 않은 파일입니다"),
    CANNOT_FOUND_S3_ERROR(HttpStatus.NOT_FOUND, "S3 저장소에서 해당하는 파일을 찾을 수 없습니다"),

    // Product
    DUPLICATED_CATEGORY_NAME_ERROR(HttpStatus.CONFLICT, "이미 해당하는 카테고리 이름이 존재합니다"),
    CANNOT_FOUND_PARENT_CATEGORY_ERROR(HttpStatus.NOT_FOUND, "부모 카테고리를 찾을 수 없습니다"),
    CATEGORY_DEPTH_EXCEEDED_ERROR(HttpStatus.BAD_REQUEST, "카테고리 기준 깊이를 넘었습니다"),
    DUPLICATED_PRODUCT_NAME_ERROR(HttpStatus.CONFLICT, "동일한 제품명을 사용할 수 없습니다"),
    CANNOT_FOUND_CATEGORY_ERROR(HttpStatus.NOT_FOUND, "해당하는 카테고리를 찾을 수 없습니다"),
    OUT_OF_STOCK(HttpStatus.CONFLICT, "재고가 부족합니다"),
    OUT_OF_OPTION_INDEX(HttpStatus.BAD_REQUEST, "잘못된 옵션 인덱스를 입력했습니다"),
    EXCEEDED_IMAGE_LIMIT_ERROR(HttpStatus.BAD_REQUEST, "상세 이미지는 최대 5장까지 업로드 가능합니다"),

    // Review
    ALREADY_REVIEW_EXISTS(HttpStatus.CONFLICT, "이미 해당 상품에 대한 리뷰를 작성했습니다."),
    PRODUCT_REVIEW_NOT_FOUND_ERROR(HttpStatus.NOT_FOUND, "해당하는 제품 리뷰를 찾을 수 없습니다"),
    PRODUCT_REVIEW_FORBIDDEN(HttpStatus.BAD_REQUEST, "본인이 작성한 리뷰만 수정할 수 있습니다"),
    PRODUCT_NOT_FOUND_ERROR(HttpStatus.NOT_FOUND, "해당하는 제품을 찾을 수 없습니다"),

    // Community
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당하는 사용자를 찾을 수 없습니다"),
    COMMUNITY_POST_NOT_FOUND(HttpStatus.NOT_FOUND, "해당하는 게시글을 찾을 수 없습니다"),
    COMMUNITY_POST_FORBIDDEN(HttpStatus.FORBIDDEN, "본인이 작성한 게시글만 수정/삭제할 수 있습니다"),
    COMMUNITY_COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당하는 댓글을 찾을 수 없습니다"),
    COMMUNITY_COMMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "본인이 작성한 댓글만 수정/삭제할 수 있습니다");


    private final HttpStatus status;
    private final String message;
}
