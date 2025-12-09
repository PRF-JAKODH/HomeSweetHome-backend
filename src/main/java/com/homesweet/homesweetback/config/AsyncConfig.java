package com.homesweet.homesweetback.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 처리 설정
 * 좋아요, 조회수 증가 등 응답 시간에 영향을 주지 않는 작업을 비동기로 처리
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 커뮤니티 비동기 작업용 Executor
     */
    @Bean(name = "communityTaskExecutor")
    public Executor communityTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);           // 기본 스레드 수
        executor.setMaxPoolSize(50);            // 최대 스레드 수
        executor.setQueueCapacity(100);         // 큐 크기
        executor.setThreadNamePrefix("community-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
