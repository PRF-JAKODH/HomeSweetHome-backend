package com.homesweet.homesweetback.common.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@Profile("test")
@EnableAsync
public class TestAsyncConfig {
    @Bean
    @Primary
    public Executor notificationTaskExecutor() {
        return new SyncTaskExecutor();
    }
}
