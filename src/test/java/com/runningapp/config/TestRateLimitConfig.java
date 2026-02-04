package com.runningapp.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.time.Duration;
import java.util.Map;

/**
 * 테스트 환경용 Rate Limit 설정
 *
 * 테스트에서는 Rate Limit을 매우 높게 설정하여
 * 테스트가 정상적으로 실행되도록 함
 */
@Configuration
@Profile("test")
public class TestRateLimitConfig {

    @Bean
    @Primary
    public RateLimitConfig rateLimitConfig() {
        return new HighLimitRateLimitConfig();
    }

    /**
     * 테스트용 Rate Limit 설정 - 매우 높은 제한 (분당 100만회)
     */
    private static class HighLimitRateLimitConfig extends RateLimitConfig {
        private final Bucket highLimitBucket;

        public HighLimitRateLimitConfig() {
            // 분당 100만회 허용 (테스트에서 충분)
            Bandwidth limit = Bandwidth.classic(
                    1_000_000,
                    Refill.greedy(1_000_000, Duration.ofMinutes(1))
            );
            this.highLimitBucket = Bucket.builder().addLimit(limit).build();
        }

        @Override
        public boolean isEnabled() {
            return false;  // 테스트에서는 비활성화
        }

        @Override
        public Bucket resolveBucket(String clientIp) {
            return highLimitBucket;
        }

        @Override
        public Bucket resolveLoginBucket(String clientIp) {
            return highLimitBucket;
        }

        @Override
        public Bucket resolveSignupBucket(String clientIp) {
            return highLimitBucket;
        }

        @Override
        public Map<String, String> getRateLimitPolicies() {
            return Map.of("test", "1000000 requests/minute (test mode)");
        }
    }
}
