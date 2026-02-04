package com.runningapp.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 테스트 환경용 캐시 설정 (Redis 없이 인메모리 캐시 사용)
 */
@Configuration
@EnableCaching
@Profile("test")
public class TestCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                "activitySummary",
                "activityStats",
                "activeChallenges",
                "recommendedChallenges",
                "plans",
                "recommendedPlans",
                "planSchedule"
        );
    }
}
