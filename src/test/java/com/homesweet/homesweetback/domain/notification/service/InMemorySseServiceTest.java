package com.homesweet.homesweetback.domain.notification.service;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.homesweet.homesweetback.domain.notification.service.impl.InMemorySseService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DisplayName("InMemorySseService 테스트")
@SpringBootTest
class InMemorySseServiceTest {

    @Autowired
    private InMemorySseService inMemorySseService;

    @AfterEach
    void tearDown() {
        // 각 테스트 후 SseEmitter 정리
        // InMemorySseService는 인스턴스가 재사용되므로 명시적 정리가 필요할 수 있음
    }

    @Test
    @DisplayName("SSE 연결 테스트 - 기본 연결")
    void testSseConnection() {
        // Given
        Long userId = 1L;
        
        // When
        SseEmitter sseEmitter = inMemorySseService.subscribe(userId);

        // Then
        assertThat(sseEmitter).isNotNull();
        assertThat(sseEmitter.getTimeout()).isEqualTo(1000000L);
    }
}
