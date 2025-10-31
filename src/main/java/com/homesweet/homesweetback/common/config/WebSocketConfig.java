package com.homesweet.homesweetback.common.config;

import com.homesweet.homesweetback.common.config.interceptor.ChatPreHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;



@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

//    private final AuthHandshakeInterceptor authHandshakeInterceptor;
    private final ChatPreHandler chatPreHandler;
    private final ChannelInterceptor channelInterceptor;

    // 엔드포인트 등록 설정
    @Override
    public void registerStompEndpoints(StompEndpointRegistry config) {

        config.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
//                .setAllowedOrigins("*");
//                .addInterceptors(authHandshakeInterceptor)
                .withSockJS();
    }

    // sub : 구독, pub : 메시지 송신
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/pub");           // 클라 -> 서버(전송)
        registry.enableSimpleBroker("/sub");        // 서버 -> 클라(구독)
    }

//     대기시간 최대 15초, 메세지 사이즈 8KB, 버퍼 1.5MB
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration){
        registration.setMessageSizeLimit(8192)
                .setSendTimeLimit(15 * 1000)
                .setSendBufferSizeLimit(3 * 512 * 1024);
    }

    public void inboundChannel(ChannelRegistration registration) {
        registration.interceptors(chatPreHandler);
    }

}
