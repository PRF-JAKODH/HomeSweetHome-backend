package com.homesweet.homesweetback.common.exception;

/**
 * 토스페이먼츠 API 연동 실패 시 발생하는 커스텀 예외
 * (RuntimeException을 상속받아 Unchecked Exception으로 동작)
 */
public class TossApiFailedException extends RuntimeException {

    public TossApiFailedException(String message) {
        super(message);
    }

    // (선택 사항) 원인 에러(cause)까지 함께 넘기고 싶을 때 사용
    public TossApiFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}