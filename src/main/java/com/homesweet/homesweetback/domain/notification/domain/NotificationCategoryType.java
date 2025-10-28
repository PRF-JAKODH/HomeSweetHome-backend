package com.homesweet.homesweetback.domain.notification.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 알림 카테고리 타입
 * 
 * @author dogyungkim
 */
@Getter
@RequiredArgsConstructor
public enum NotificationCategoryType {
    ORDER("ORDER", "주문", 1L),
    PAYMENT("PAYMENT", "결제", 2L),
    COMMUNITY("COMMUNITY", "커뮤니티", 3L),
    SETTLEMENT("SETTLEMENT", "정산", 4L),
    PRODUCT("PRODUCT", "상품", 5L),
    CHAT("CHAT", "채팅", 6L),
    SYSTEM("SYSTEM", "시스템", 7L),
    PROMOTION("PROMOTION", "프로모션", 8L),
    CUSTOM("CUSTOM", "커스텀", 9L);

    private final String code;
    private final String description;
    private final Long categoryId;

    public static NotificationCategoryType fromCode(String code) {
        for (NotificationCategoryType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown notification category type: " + code);
    }
}
