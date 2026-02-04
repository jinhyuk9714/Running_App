package com.runningapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄링 설정
 *
 * - @EnableScheduling: @Scheduled 메서드 주기적 실행 활성화
 * - 테스트 환경에서는 비활성화 (예측 불가능한 스케줄링 방지)
 */
@Configuration
@EnableScheduling
@Profile("!test")
public class SchedulingConfig {
}
