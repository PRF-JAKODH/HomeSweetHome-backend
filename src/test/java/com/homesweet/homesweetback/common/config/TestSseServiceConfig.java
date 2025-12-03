package com.homesweet.homesweetback.common.config;

import com.homesweet.homesweetback.domain.notification.service.SseService;
import com.homesweet.homesweetback.domain.notification.dto.PushNotificationDTO;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 테스트 환경용 SSE 서비스 설정
 * 
 * 실제 SSE 연결 없이도 테스트가 가능하도록 하는 NoOp 구현체를 제공합니다.
 * 테스트 프로파일이 활성화될 때 자동으로 사용되며, 실제 SSE 전송 없이 동작합니다.
 */
@Configuration
@Profile("test")
public class TestSseServiceConfig {

    @Bean
    @Primary
    public SseService testSseService() {
        return new NoOpSseService();
    }

    /**
     * 테스트용 NoOp SSE 서비스 구현체
     * 실제 SSE 전송 없이 동작하며, 테스트에서 SSE 연결 없이도 정상 동작하도록 합니다.
     */
    private static class NoOpSseService implements SseService {

        @Override
        public SseEmitter subscribe(Long userId) {
            // 테스트 환경에서는 실제 SseEmitter를 생성하지 않고 null을 반환하거나
            // 더미 SseEmitter를 반환할 수 있습니다.
            // 하지만 일반적으로 테스트에서는 subscribe 메서드가 호출되지 않으므로
            // 간단한 더미 객체를 반환합니다.
            SseEmitter emitter = new SseEmitter(1000L);
            // 즉시 완료시켜서 리소스 정리
            emitter.complete();
            return emitter;
        }

        @Override
        public void sendNotification(Long userId, PushNotificationDTO data) {
            // 테스트 환경에서는 실제 SSE 전송 없이 동작하므로 아무것도 하지 않습니다.
        }

        @Override
        public void sendNotifications(Map<Long, PushNotificationDTO> notifications) {
            // 테스트 환경에서는 실제 SSE 전송 없이 동작하므로 아무것도 하지 않습니다.
        }
    }
}
