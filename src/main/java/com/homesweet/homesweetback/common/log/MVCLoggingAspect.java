package com.homesweet.homesweetback.common.log;

/**
 * 컨트롤러·서비스·리포지토리 전 구간의 요청·응답·예외를 일관된 포맷으로 로깅 처리
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 18.
 */
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class MVCLoggingAspect {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TRACE_ID = "traceId";
    private static final String PREFIX_START = "-->";
    private static final String PREFIX_COMPLETE = "<--";
    private static final String PREFIX_COMPLETE_ERROR = "<-X-";
    private static final String MDC_DEPTH = "traceDepth";

    /**
     * Controller, Service, Repository 메서드를 모두 감싼 포인트컷
     */
    @Around("execution(* com.homesweet.homesweetback..*Controller.*(..)) || " +
            "execution(* com.homesweet.homesweetback..*Service.*(..)) || " +
            "execution(* com.homesweet.homesweetback..*Repository.*(..))")
    public Object logAllLayers(ProceedingJoinPoint joinPoint) throws Throwable {
        initTraceIdIfAbsent();

        int depth = incDepth();
        boolean isRoot = depth == 1;

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();

        int indentLevel = getIndentLevel(className);
        String layerTag = getLayerTag(className);

        String prefixStart = addIndent(PREFIX_START, indentLevel);
        String prefixComplete = addIndent(PREFIX_COMPLETE, indentLevel);

        Object[] args = joinPoint.getArgs();
        String argsJson = toJson(args);

        if (isRoot) {
            log.info("{} ============================== [START REQUEST] {}.{} ==============================", getTraceId(), className, methodName);
        }

        log.info("{} {} [Request] ClassName: {}.{} - Method: {}() - Args: {}", getTraceId(),
                prefixStart, layerTag,
                className, methodName, argsJson);

        try {
            long startTime = System.currentTimeMillis();
            Object result = joinPoint.proceed(args);
            long duration = System.currentTimeMillis() - startTime;

            String resultJson = toJson(result);
            log.info("{} {} [Response] ClassName: {}.{} - Method: {}() - Result: {} ({} ms)",
                    getTraceId(),
                    prefixComplete, layerTag, className, methodName, resultJson, duration);

            if (isRoot) {
                log.info("{} ============================== [END REQUEST] {}.{} ==============================", getTraceId(), className, methodName);
                clearTraceIdIfRootLayer(className);
            }
            return result;
        } catch (Exception e) {
            log.error("{} {} [Exception] ClassName: {}.{} - Method: {} - ErrorMessage: {}", getTraceId(),
                    PREFIX_COMPLETE_ERROR, layerTag, className,
                    methodName, e.toString(), e);

            if (isRoot) {
                log.info("{} ============================== [END REQUEST WITH ERROR] {}.{} ==============================", getTraceId(), className, methodName);
                clearTraceIdIfRootLayer(className);
            }
            throw e;
        } finally {
            decDepth();
        }
    }

    /**
     * 로그에 표시할 TraceId를 MDC에서 가져오거나, 없으면 새로 생성해 세팅
     */
    private void initTraceIdIfAbsent() {
        if (MDC.get(TRACE_ID) == null) {
            MDC.put(TRACE_ID, UUID.randomUUID().toString());
        }
    }

    /**
     * 요청이 최상위 계층(컨트롤러)에서 끝났으면 MDC에서 TraceId 제거
     */
    private void clearTraceIdIfRootLayer(String className) {
        if (className.contains("Controller")) {
            MDC.remove(TRACE_ID);
        }
    }

    private String getTraceId() {
        String id = MDC.get(TRACE_ID);
        return (id != null) ? "[TraceId:" + id + "]" : "";
    }

    private String getLayerTag(String className) {
        if (className.contains("Controller")) {
            return "[CONTROLLER]";
        }
        if (className.contains("Service")) {
            return "[SERVICE]";
        }
        if (className.contains("Repository")) {
            return "[REPOSITORY]";
        }
        return "[OTHER]";
    }

    private int getIndentLevel(String className) {
        if (className.contains("Controller")) {
            return 0;
        }
        if (className.contains("Service")) {
            return 1;
        }
        if (className.contains("Repository")) {
            return 2;
        }
        return 0;
    }

    private String addIndent(String prefix, int indentLevel) {
        return "   ".repeat(indentLevel) + prefix;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }

    private int incDepth() {
        String v = MDC.get(MDC_DEPTH);
        int d = (v == null) ? 0 : Integer.parseInt(v);
        d++;
        MDC.put(MDC_DEPTH, Integer.toString(d));
        return d;
    }

    private void decDepth() {
        String v = MDC.get(MDC_DEPTH);
        if (v == null) return;
        int d = Integer.parseInt(v) - 1;
        if (d <= 0) MDC.remove(MDC_DEPTH);
        else MDC.put(MDC_DEPTH, Integer.toString(d));
    }
}
