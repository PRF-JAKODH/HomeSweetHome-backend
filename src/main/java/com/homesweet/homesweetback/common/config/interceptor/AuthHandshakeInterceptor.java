//package com.homesweet.homesweetback.common.config.interceptor;
//
//import com.homesweet.homesweetback.common.security.jwt.JwtTokenProvider;
//import com.homesweet.homesweetback.domain.auth.entity.UserRole;
//import io.jsonwebtoken.Claims;
//import io.jsonwebtoken.JwtException;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.server.ServerHttpRequest;
//import org.springframework.http.server.ServerHttpResponse;
//import org.springframework.stereotype.Component;
//import org.springframework.web.socket.WebSocketHandler;
//import org.springframework.web.socket.server.HandshakeInterceptor;
//
//import java.util.Map;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class AuthHandshakeInterceptor implements HandshakeInterceptor {
//
//    private final JwtTokenProvider jwtTokenProvider;
//
//
//    public boolean beforeHandshake(ServerHttpRequest request,
//                                   ServerHttpResponse response,
//                                   WebSocketHandler wsHandler,
//                                   Map<String, Object> attributes) {
//
//        String authorizationHeader = request.getHeaders().getFirst("Authorization");
//        String jwt = request.getURI().getQuery();
//        log.info("JWT: {}",jwt);
//        log.info(request.getURI().toString());
//
//        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
//            log.warn("웹소켓 연결 불가 : Authorization 헤더 없음");
//            return false;
//        }
//
//        String token = authorizationHeader.substring(7);
//
//        try {
//
//            Claims claims = jwtTokenProvider.getClaimsFromToken(token);
//            Long userId = Long.valueOf(claims.getSubject());
//            UserRole role = claims.get("role", UserRole.class);
//
//                attributes.put("userId", userId);
//                attributes.put("role", role);
//
//                return true;
//
//        } catch (JwtException e) {
//            log.warn("websocket 연결 거부: {} " + e.getMessage());
//            return false;
//        } catch (Exception e) {
//            log.error("handshake 실패: {} " + e.getMessage(), e);
//            return false;
//        }
//    }
//
//    // 후처리
//    public void afterHandshake(ServerHttpRequest request,
//                               ServerHttpResponse response,
//                               WebSocketHandler wsHandler,
//                               Exception exception) {
//        if (exception != null) {
//            log.error("후처리 에러 : {} ", exception.getMessage());
//        }
//
//    }
//
//}