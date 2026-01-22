package com.homesweet.homesweetback.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;



@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // 엔드포인트 등록 설정
    @Override
    public void registerStompEndpoints(StompEndpointRegistry config) {

        config.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:3000")
                .withSockJS();


        // 부하테스트용 (순수 WebSocket)
        config.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns("*");
    }

    // sub : 구독, pub : 메시지 송신
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");        // 서버 -> 클라(구독)
        registry.setApplicationDestinationPrefixes("/app");           // 클라 -> 서버(전송)
    }

//     대기시간 최대 15초, 메세지 사이즈 8KB, 버퍼 1.5MB
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration){
        registration.setMessageSizeLimit(8192)
                .setSendTimeLimit(15 * 1000)
                .setSendBufferSizeLimit(3 * 512 * 1024);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        int cores = Runtime.getRuntime().availableProcessors();

        registration.taskExecutor()
                .corePoolSize(cores * 4)   // 기본 스레드 풀 크기 (CPU 경합 완화)
                .maxPoolSize(cores * 8)    // 최대 스레드 풀 크기 (1만 명 부하 흡수)
                .queueCapacity(10000);
    }


    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.taskExecutor()
                .corePoolSize(16)
                .maxPoolSize(32);

    }

}

