package com.homesweet.homesweetback.domain.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("NotificationCategoryType 테스트")
public class NotificationCategoryTypeTest {
    private final NotificationCategoryType categoryType = NotificationCategoryType.ORDER;

    @Test
    @DisplayName("NotificationCategoryType 테스트")
    void testNotificationCategoryType() {
        assertThat(categoryType.getDescription()).isEqualTo("주문");
        assertThat(categoryType.getCode()).isEqualTo("ORDER");
    }

    @Test
    @DisplayName("NotificationCategoryType fromCode 테스트_성공")
    void testNotificationCategoryTypeFromCode_Success() {
        assertThat(NotificationCategoryType.fromCode("ORDER")).isEqualTo(categoryType);
    }
    
    @Test
    @DisplayName("NotificationCategoryType fromCode 테스트_실패")
    void testNotificationCategoryTypeFromCode_Failure() {
        assertThatThrownBy(() -> NotificationCategoryType.fromCode("INVALID_CATEGORY_TYPE")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("NotificationCategoryType fromCategoryId 테스트_성공")
    void testNotificationCategoryTypeFromCategoryId_Success() {
        assertThat(NotificationCategoryType.fromCategoryId(1L)).isEqualTo(categoryType);
    }
    
    @Test
    @DisplayName("NotificationCategoryType fromCategoryId 테스트_실패")
    void testNotificationCategoryTypeFromCategoryId_Failure() {
        assertThatThrownBy(() -> NotificationCategoryType.fromCategoryId(999L)).isInstanceOf(IllegalArgumentException.class);
    }
}
