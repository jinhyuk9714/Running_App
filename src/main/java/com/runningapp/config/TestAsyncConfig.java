package com.runningapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;

/**
 * 테스트용 비동기 설정
 *
 * SyncTaskExecutor: @Async 메서드를 동기적으로 실행하여 테스트 결과 예측 가능
 */
@Configuration
@EnableAsync
@EnableRetry
@Profile("test")
public class TestAsyncConfig {

    @Bean(name = "taskExecutor")
    @Primary
    public Executor taskExecutor() {
        return new SyncTaskExecutor();
    }
}
