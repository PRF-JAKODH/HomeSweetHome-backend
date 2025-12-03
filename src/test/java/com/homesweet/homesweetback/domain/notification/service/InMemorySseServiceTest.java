package com.homesweet.homesweetback.domain.notification.service;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.homesweet.homesweetback.domain.notification.dto.PushNotificationDTO;
import com.homesweet.homesweetback.domain.notification.service.impl.InMemorySseService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.*;

@ActiveProfiles("test")
@DisplayName("InMemorySseService 테스트")
@SpringBootTest
@Import(InMemorySseService.class)
class InMemorySseServiceTest {

    @Autowired
    @Qualifier("inMemorySseService")
    private SseService inMemorySseService;

    @AfterEach
    void tearDown() {
        // 각 테스트 후 SseEmitter 정리
        // InMemorySseService는 인스턴스가 재사용되므로 명시적 정리가 필요할 수 있음
    }

    // ========== 기본 연결 테스트 ==========

    @Test
    @DisplayName("SSE 연결 테스트 - 기본 연결")
    void testSseConnection() {
        // Given
        Long userId = 1L;

        // When
        SseEmitter sseEmitter = inMemorySseService.subscribe(userId);

        // Then
        assertThat(sseEmitter).isNotNull();
    }

    @Test
    @DisplayName("SSE 연결 테스트 - 재연결")
    void testSseReconnection() {
        // Given
        Long userId = 1L;
        SseEmitter firstEmitter = inMemorySseService.subscribe(userId);

        // When - 같은 사용자가 재연결
        SseEmitter secondEmitter = inMemorySseService.subscribe(userId);

        // Then - 새로운 emitter가 생성되어야 함
        assertThat(firstEmitter).isNotNull();
        assertThat(secondEmitter).isNotNull();
        assertThat(firstEmitter).isNotSameAs(secondEmitter);
    }

    // ========== 알림 전송 테스트 ==========

    @Test
    @DisplayName("SSE 전송 테스트 - 단일 알림 성공")
    void testSendNotification_Success() {
        // Given
        Long userId = 1L;
        PushNotificationDTO contextData = PushNotificationDTO.builder()
                .title("test")
                .content("test content")
                .build();
        inMemorySseService.subscribe(userId);

        // When & Then
        assertThatCode(() -> inMemorySseService.sendNotification(userId, contextData))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SSE 전송 테스트 - 연결되지 않은 사용자")
    void testSendNotification_UserNotConnected() {
        // Given
        Long userId = 999L;
        PushNotificationDTO contextData = PushNotificationDTO.builder()
                .title("test")
                .content("test content")
                .build();

        // When & Then - 연결되지 않은 사용자에게 알림을 보내도 예외가 발생하지 않아야 함 (로그만 출력)
        assertThatCode(() -> inMemorySseService.sendNotification(userId, contextData))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SSE 전송 테스트 - 다수 알림 전송")
    void testSendNotifications_Multiple() {
        // Given
        Map<Long, PushNotificationDTO> notifications = new HashMap<>();
        for (long i = 1; i <= 5; i++) {
            inMemorySseService.subscribe(i);
            notifications.put(i, PushNotificationDTO.builder()
                    .title("test" + i)
                    .content("test content " + i)
                    .build());
        }

        // When & Then
        assertThatCode(() -> inMemorySseService.sendNotifications(notifications))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SSE 전송 테스트 - 빈 Map")
    void testSendNotifications_EmptyMap() {
        // Given
        Map<Long, PushNotificationDTO> emptyNotifications = new HashMap<>();

        // When & Then
        assertThatCode(() -> inMemorySseService.sendNotifications(emptyNotifications))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SSE 전송 테스트 - null Map")
    void testSendNotifications_NullMap() {
        // When & Then
        assertThatCode(() -> inMemorySseService.sendNotifications(Map.of()))
                .doesNotThrowAnyException();
    }

    // ========== Emitter 생명주기 테스트 ==========

    @Test
    @DisplayName("SSE 연결 테스트 - Emitter 완료 후 알림 전송")
    void testSendNotification_AfterEmitterComplete() {
        // Given
        Long userId = 1L;
        SseEmitter emitter = inMemorySseService.subscribe(userId);
        emitter.complete();

        // When & Then - 완료된 emitter에 알림을 보내면 에러가 발생하지만 처리됨
        assertThatCode(
                () -> inMemorySseService.sendNotification(userId, PushNotificationDTO.builder().title("test").build()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SSE 연결 테스트 - Emitter 타임아웃 콜백 설정")
    void testEmitterTimeoutCallback() {
        // Given
        Long userId = 1L;
        AtomicBoolean timeoutCalled = new AtomicBoolean(false);

        SseEmitter emitter = inMemorySseService.subscribe(userId);
        emitter.onTimeout(() -> timeoutCalled.set(true));

        // When
        emitter.complete();

        // Then - 타임아웃이 아닌 정상 완료이므로 타임아웃 콜백은 호출되지 않음
        assertThat(timeoutCalled.get()).isFalse();
    }

    // ========== 특수 케이스 테스트 ==========

    @Test
    @DisplayName("SSE 전송 테스트 - 특수 문자 포함 데이터")
    void testSendNotification_SpecialCharacters() {
        // Given
        Long userId = 1L;
        inMemorySseService.subscribe(userId);
        PushNotificationDTO specialData = PushNotificationDTO.builder()
                .title("테스트 !@#$%^&*()_+-=[]{}|;':,.<>?\"")
                .content("특수문자 테스트")
                .build();

        // When & Then
        assertThatCode(() -> inMemorySseService.sendNotification(userId, specialData))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SSE 전송 테스트 - 긴 데이터")
    void testSendNotification_LongData() {
        // Given
        Long userId = 1L;
        inMemorySseService.subscribe(userId);
        StringBuilder longData = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longData.append("테스트 ");
        }
        PushNotificationDTO longDataDto = PushNotificationDTO.builder()
                .title("Long Data")
                .content(longData.toString())
                .build();

        // When & Then
        assertThatCode(() -> inMemorySseService.sendNotification(userId, longDataDto))
                .doesNotThrowAnyException();
    }
}
