package com.homesweet.homesweetback.domain.notification.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.homesweet.homesweetback.domain.notification.domain.NotificationTemplateType;

@DisplayName("SystemNotification 테스트")
public class SystemNotificationTest {

    @Test
    @DisplayName("SystemMaintenance 생성 테스트_성공")
    void testCreateSystemMaintenance() {
        // Given
        SystemNotification.SystemMaintenance systemMaintenance = SystemNotification.SystemMaintenance.builder()
            .maintenanceTime("2024-01-01 00:00 ~ 02:00")
            .build();

        // Then
        assertThat(systemMaintenance.getMaintenanceTime()).isEqualTo("2024-01-01 00:00 ~ 02:00");
        assertThat(systemMaintenance.getEventType()).isEqualTo(NotificationTemplateType.SYSTEM_MAINTENANCE);
    }

    @Test
    @DisplayName("SystemMaintenance 생성 테스트_실패_maintenanceTime_null")
    void testCreateSystemMaintenance_Failure_MaintenanceTimeNull() {
        // When & Then
        assertThatThrownBy(() -> SystemNotification.SystemMaintenance.builder()
            .maintenanceTime(null)
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("SystemUpdate 생성 테스트_성공")
    void testCreateSystemUpdate() {
        // Given
        SystemNotification.SystemUpdate systemUpdate = SystemNotification.SystemUpdate.builder()
            .version("1.2.0")
            .updateFeatures("버그 수정, 성능 개선")
            .build();

        // Then
        assertThat(systemUpdate.getVersion()).isEqualTo("1.2.0");
        assertThat(systemUpdate.getUpdateFeatures()).isEqualTo("버그 수정, 성능 개선");
        assertThat(systemUpdate.getEventType()).isEqualTo(NotificationTemplateType.SYSTEM_UPDATE);
    }

    @Test
    @DisplayName("SystemUpdate 생성 테스트_실패_version_null")
    void testCreateSystemUpdate_Failure_VersionNull() {
        // When & Then
        assertThatThrownBy(() -> SystemNotification.SystemUpdate.builder()
            .version(null)
            .updateFeatures("버그 수정, 성능 개선")
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("SystemUpdate 생성 테스트_실패_updateFeatures_blank")
    void testCreateSystemUpdate_Failure_UpdateFeaturesBlank() {
        // When & Then
        assertThatThrownBy(() -> SystemNotification.SystemUpdate.builder()
            .version("1.2.0")
            .updateFeatures("")
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("SellerRegistrationComplete 생성 테스트_성공")
    void testCreateSellerRegistrationComplete() {
        // Given
        SystemNotification.SellerRegistrationComplete sellerRegistrationComplete = SystemNotification.SellerRegistrationComplete.builder()
            .userName("홍길동")
            .build();

        // Then
        assertThat(sellerRegistrationComplete.getUserName()).isEqualTo("홍길동");
        assertThat(sellerRegistrationComplete.getEventType()).isEqualTo(NotificationTemplateType.SELLER_REGISTRATION_COMPLETE);
    }

    @Test
    @DisplayName("SellerRegistrationComplete 생성 테스트_실패_userName_blank")
    void testCreateSellerRegistrationComplete_Failure_UserNameBlank() {
        // When & Then
        assertThatThrownBy(() -> SystemNotification.SellerRegistrationComplete.builder()
            .userName("")
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }
}

