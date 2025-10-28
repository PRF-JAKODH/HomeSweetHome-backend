package com.homesweet.homesweetback.common.config;

import com.homesweet.homesweetback.common.config.interceptor.AuthHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // 엔드포인트 등록 설정
    @Override
    public void registerStompEndpoints(StompEndpointRegistry config) {
        config.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:8080")
                .withSockJS();
    }

    // sub : 구독, pub : 메시지 송신
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/sub");        // 서버 -> 클라(구독)
        registry.setApplicationDestinationPrefixes("/pub");           // 클라 -> 서버(전송)
    }

    // 대기시간 최대 15초, 메세지 사이즈 8KB, 버퍼 1.5MB
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration){
        registration.setMessageSizeLimit(8192)
                .setSendTimeLimit(15 * 1000)
                .setSendBufferSizeLimit(3 * 512 * 1024);
    }


}
