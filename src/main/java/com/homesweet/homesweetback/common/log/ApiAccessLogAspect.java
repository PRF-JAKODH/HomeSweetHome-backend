package com.homesweet.homesweetback.common.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homesweet.homesweetback.domain.auth.service.UserLogProducer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jboss.logging.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 사용자 행동 분석용 로그
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 10.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
class ApiAccessLogAspect {

    private final UserLogProducer producer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object logApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

        String traceId = UUID.randomUUID().toString();
        MDC.put("traceId", traceId);

        Object result = null;
        String status = "SUCCESS";
        String exceptionType = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            status = "FAILURE";
            exceptionType = e.getClass().getSimpleName();
            throw e;
        } finally {
            long end = System.currentTimeMillis();

            Map<String, Object> logData = new HashMap<>();
            logData.put("timestamp", LocalDateTime.now().toString());
            logData.put("traceId", traceId);
            logData.put("method", request.getMethod());
            logData.put("uri", request.getRequestURI());
            logData.put("executionTime", end - start);
            logData.put("clientIp", request.getRemoteAddr());
            logData.put("userAgent", request.getHeader("User-Agent"));
            logData.put("status", status);
            logData.put("exceptionType", exceptionType);
            logData.put("controllerMethod",
                    joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName());

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                logData.put("userId", auth.getName());
            }

            producer.sendLog(objectMapper.writeValueAsString(logData));
            MDC.clear();
        }
    }
}
