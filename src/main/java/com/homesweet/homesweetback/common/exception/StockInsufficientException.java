package com.homesweet.homesweetback.common.exception;

// "재고 부족" 상황에 사용할 커스텀 예외 (400 Bad Request 또는 409 Conflict용)
public class StockInsufficientException extends RuntimeException {
    public StockInsufficientException(String message) {
        super(message);
    }
}