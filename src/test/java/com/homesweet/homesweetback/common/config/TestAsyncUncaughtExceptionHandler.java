package com.homesweet.homesweetback.common.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

// 테스트에서 사용하기 위해 @Component로 등록하거나 테스트 설정에 빈으로 등록합니다.
// 여기서는 테스트 설정에서 빈으로 등록하는 것을 추천합니다.
public class TestAsyncUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {

    // 스레드로부터 안전하게 예외를 저장하기 위해 AtomicReference 사용
    private final AtomicReference<Throwable> capturedException = new AtomicReference<>();

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        // 예외가 발생하면 여기로 잡혀옵니다.
        // 예외를 저장합니다.
        capturedException.set(ex);
    }

    public Throwable getCapturedException() {
        return capturedException.get();
    }

    public void clear() {
        capturedException.set(null);
    }
}