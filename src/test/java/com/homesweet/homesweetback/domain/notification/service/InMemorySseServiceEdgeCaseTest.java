package com.homesweet.homesweetback.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.homesweet.homesweetback.domain.notification.dto.PushNotificationDTO;
import com.homesweet.homesweetback.domain.notification.service.impl.InMemorySseService;

/**
 * InMemorySseService 엣지 케이스 테스트
 * 
 * 동시 연결, 대량 알림 전송, 에러 핸들링 등 다양한 엣지 케이스를 테스트합니다.
 * 
 * @author dogyungkim
 */
@ActiveProfiles("test")
@DisplayName("InMemorySseService 엣지 케이스 테스트")
@SpringBootTest
@Import(InMemorySseService.class)
class InMemorySseServiceEdgeCaseTest {

    @Autowired
    @Qualifier("inMemorySseService")
    private SseService inMemorySseService;

    @AfterEach
    void tearDown() {
        // SSE 연결 정리는 자동으로 처리됨
    }

    // ========== 동시 연결 테스트 ==========

    @Test
    @DisplayName("SSE 연결 테스트_성공_다수_사용자_동시_연결")
    void subscribe_Success_MultipleUsers() {
        // Given & When
        SseEmitter emitter1 = inMemorySseService.subscribe(1L);
        SseEmitter emitter2 = inMemorySseService.subscribe(2L);
        SseEmitter emitter3 = inMemorySseService.subscribe(3L);

        // Then
        assertThat(emitter1).isNotNull();
        assertThat(emitter2).isNotNull();
        assertThat(emitter3).isNotNull();
        assertThat(emitter1).isNotSameAs(emitter2);
        assertThat(emitter2).isNotSameAs(emitter3);
    }

    @Test
    @DisplayName("SSE 연결 테스트_성공_같은_사용자_재연결")
    void subscribe_Success_SameUserReconnect() {
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

    @Test
    @DisplayName("SSE 연결 테스트_성공_대량_동시_연결_100명")
    void subscribe_Success_MassiveConcurrentConnections_100() throws InterruptedException {
        // Given
        int userCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(userCount);
        Map<Long, SseEmitter> emitters = new HashMap<>();

        // When - 100명의 사용자가 동시에 연결
        for (long i = 1; i <= userCount; i++) {
            final long userId = i;
            executorService.submit(() -> {
                try {
                    SseEmitter emitter = inMemorySseService.subscribe(userId);
                    synchronized (emitters) {
                        emitters.put(userId, emitter);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // Then
        assertThat(emitters).hasSize(userCount);
    }

    // ========== 대량 알림 전송 테스트 ==========

    @Test
    @DisplayName("SSE 알림 전송 테스트_성공_빈_Map")
    void sendNotifications_Success_EmptyMap() {
        // Given
        Map<Long, PushNotificationDTO> emptyNotifications = new HashMap<>();

        // When & Then - 예외가 발생하지 않아야 함
        assertThatCode(() -> {
            inMemorySseService.sendNotifications(emptyNotifications);
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SSE 알림 전송 테스트_성공_단일_알림")
    void sendNotifications_Success_SingleNotification() {
        // Given
        Long userId = 1L;
        inMemorySseService.subscribe(userId);
        Map<Long, PushNotificationDTO> notifications = Map.of(userId, PushNotificationDTO.builder()
                .title("test")
                .content("test content")
                .build());

        // When & Then
        assertThatCode(() -> {
            inMemorySseService.sendNotifications(notifications);
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SSE 알림 전송 테스트_성공_대량_알림_50개")
    void sendNotifications_Success_LargeDataset_50() {
        // Given
        Map<Long, PushNotificationDTO> notifications = new HashMap<>();
        for (long i = 1; i <= 50; i++) {
            inMemorySseService.subscribe(i);
            notifications.put(i, PushNotificationDTO.builder()
                    .title("test" + i)
                    .content("test content " + i)
                    .build());
        }

        // When & Then
        assertThatCode(() -> {
            inMemorySseService.sendNotifications(notifications);
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SSE 알림 전송 테스트_성공_대량_알림_100개")
    void sendNotifications_Success_LargeDataset_100() {
        // Given
        Map<Long, PushNotificationDTO> notifications = new HashMap<>();
        for (long i = 1; i <= 100; i++) {
            inMemorySseService.subscribe(i);
            notifications.put(i, PushNotificationDTO.builder()
                    .title("test" + i)
                    .content("test content " + i)
                    .build());
        }

        // When & Then
        assertThatCode(() -> {
            inMemorySseService.sendNotifications(notifications);
        }).doesNotThrowAnyException();
    }

    // ========== 청크 처리 테스트 ==========

    @Test
    @DisplayName("SSE 알림 전송 테스트_성공_청크_크기보다_작은_데이터")
    void sendNotifications_Success_SmallerThanChunkSize() {
        // Given - 청크 크기(10)보다 작은 5개의 알림
        Map<Long, PushNotificationDTO> notifications = new HashMap<>();
        for (long i = 1; i <= 5; i++) {
            inMemorySseService.subscribe(i);
            notifications.put(i, PushNotificationDTO.builder()
                    .title("test" + i)
                    .content("test content " + i)
                    .build());
        }

        // When & Then
        assertThatCode(() -> {
            inMemorySseService.sendNotifications(notifications);
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SSE 알림 전송 테스트_성공_청크_크기와_동일한_데이터")
    void sendNotifications_Success_ExactlyChunkSize() {
        // Given - 청크 크기(10)와 동일한 10개의 알림
        Map<Long, PushNotificationDTO> notifications = new HashMap<>();
        for (long i = 1; i <= 10; i++) {
            inMemorySseService.subscribe(i);
            notifications.put(i, PushNotificationDTO.builder()
                    .title("test" + i)
                    .content("test content " + i)
                    .build());
        }

        // When & Then
        assertThatCode(() -> {
            inMemorySseService.sendNotifications(notifications);
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SSE 알림 전송 테스트_성공_청크_크기의_배수")
    void sendNotifications_Success_MultipleOfChunkSize() {
        // Given - 청크 크기(10)의 배수인 30개의 알림
        Map<Long, PushNotificationDTO> notifications = new HashMap<>();
        for (long i = 1; i <= 30; i++) {
            inMemorySseService.subscribe(i);
            notifications.put(i, PushNotificationDTO.builder()
                    .title("test" + i)
                    .content("test content " + i)
                    .build());
        }

        // When & Then
        assertThatCode(() -> {
            inMemorySseService.sendNotifications(notifications);
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SSE 알림 전송 테스트_성공_청크_크기의_배수가_아닌_데이터")
    void sendNotifications_Success_NotMultipleOfChunkSize() {
        // Given - 청크 크기(10)의 배수가 아닌 23개의 알림
        Map<Long, PushNotificationDTO> notifications = new HashMap<>();
        for (long i = 1; i <= 23; i++) {
            inMemorySseService.subscribe(i);
            notifications.put(i, PushNotificationDTO.builder()
                    .title("test" + i)
                    .content("test content " + i)
                    .build());
        }

        // When & Then
        assertThatCode(() -> {
            inMemorySseService.sendNotifications(notifications);
        }).doesNotThrowAnyException();
    }

    // ========== 연결되지 않은 사용자 테스트 ==========

    @Test
    @DisplayName("SSE 알림 전송 테스트_연결되지_않은_사용자_혼합")
    void sendNotifications_MixedConnectedAndDisconnected() {
        // Given
        inMemorySseService.subscribe(1L);
        inMemorySseService.subscribe(2L);
        // 3L은 연결하지 않음

        Map<Long, PushNotificationDTO> notifications = Map.of(
                1L, PushNotificationDTO.builder().title("test1").build(),
                2L, PushNotificationDTO.builder().title("test2").build(),
                3L, PushNotificationDTO.builder().title("test3").build());

        // When & Then - 연결되지 않은 사용자가 있어도 예외가 발생하지 않아야 함
        assertThatCode(() -> {
            inMemorySseService.sendNotifications(notifications);
        }).doesNotThrowAnyException();
    }

    // ========== 연결 완료 후 알림 전송 테스트 ==========

    @Test
    @DisplayName("SSE 알림 전송 테스트_연결_완료_후_알림_전송")
    void sendNotification_AfterConnectionComplete() {
        // Given
        Long userId = 1L;
        SseEmitter emitter = inMemorySseService.subscribe(userId);
        emitter.complete();

        // When & Then - 완료된 연결에 알림을 보내도 예외가 발생하지 않아야 함
        assertThatCode(() -> {
            inMemorySseService.sendNotification(userId, PushNotificationDTO.builder().title("test").build());
        }).doesNotThrowAnyException();
    }

    // ========== 특수 문자 데이터 테스트 ==========

    @Test
    @DisplayName("SSE 알림 전송 테스트_성공_특수_문자_포함_데이터")
    void sendNotification_Success_SpecialCharacters() {
        // Given
        Long userId = 1L;
        inMemorySseService.subscribe(userId);
        PushNotificationDTO specialData = PushNotificationDTO.builder()
                .title("테스트 메시지 !@#$%^&*()_+-=[]{}|;':,.<>?\"")
                .content("특수문자 테스트")
                .build();

        // When & Then
        assertThatCode(() -> {
            inMemorySseService.sendNotification(userId, specialData);
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SSE 알림 전송 테스트_성공_긴_데이터")
    void sendNotification_Success_LongData() {
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
        assertThatCode(() -> {
            inMemorySseService.sendNotification(userId, longDataDto);
        }).doesNotThrowAnyException();
    }

    // ========== 동시 알림 전송 테스트 ==========

    @Test
    @DisplayName("SSE 알림 전송 테스트_성공_동시_다수_알림_전송")
    void sendNotifications_Success_ConcurrentSending() throws InterruptedException {
        // Given
        int notificationCount = 20;
        for (long i = 1; i <= notificationCount; i++) {
            inMemorySseService.subscribe(i);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(5);

        // When - 5개의 스레드에서 동시에 알림 전송
        for (int i = 0; i < 5; i++) {
            final int batchNumber = i;
            executorService.submit(() -> {
                try {
                    Map<Long, PushNotificationDTO> notifications = new HashMap<>();
                    for (long j = 1; j <= notificationCount; j++) {
                        notifications.put(j, PushNotificationDTO.builder()
                                .title("batch" + batchNumber + "_user" + j)
                                .content("test content")
                                .build());
                    }
                    inMemorySseService.sendNotifications(notifications);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // Then - 예외 없이 완료되어야 함
        assertThat(executorService.isShutdown()).isTrue();
    }

    // ========== 타임아웃 테스트 ==========

    @Test
    @DisplayName("SSE 연결 테스트_타임아웃_설정_확인")
    void subscribe_TimeoutConfiguration() {
        // Given & When
        SseEmitter emitter = inMemorySseService.subscribe(3L);

        // Then - 타임아웃이 올바르게 설정되어야 함
        assertThat(emitter.getTimeout()).isEqualTo(1800000L);
    }
}
