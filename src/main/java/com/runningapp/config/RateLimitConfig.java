package com.runningapp.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting 설정
 *
 * Token Bucket 알고리즘 기반:
 * - 각 클라이언트(IP)별로 토큰 버킷 생성
 * - 요청 시 토큰 소비, 시간이 지나면 토큰 충전
 * - 토큰 부족 시 429 Too Many Requests 반환
 *
 * 프로퍼티:
 * - app.rate-limit.enabled: true/false (기본값: true)
 */
@Configuration
public class RateLimitConfig {

    @Value("${app.rate-limit.enabled:true}")
    private boolean enabled;

    // IP별 버킷 저장소
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> signupBuckets = new ConcurrentHashMap<>();

    // 무제한 버킷 (비활성화 시 사용)
    private final Bucket unlimitedBucket = createUnlimitedBucket();

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 일반 API Rate Limit
     * - 분당 100회
     * - 버스트 허용: 최대 120회
     */
    public Bucket resolveBucket(String clientIp) {
        if (!enabled) return unlimitedBucket;
        return buckets.computeIfAbsent(clientIp, this::createStandardBucket);
    }

    /**
     * 로그인 API Rate Limit (더 엄격)
     * - 분당 10회 (브루트포스 공격 방지)
     * - 버스트 허용: 최대 15회
     */
    public Bucket resolveLoginBucket(String clientIp) {
        if (!enabled) return unlimitedBucket;
        return loginBuckets.computeIfAbsent(clientIp, this::createLoginBucket);
    }

    /**
     * 회원가입 API Rate Limit
     * - 시간당 5회 (스팸 계정 생성 방지)
     * - 버스트 허용: 최대 10회
     */
    public Bucket resolveSignupBucket(String clientIp) {
        if (!enabled) return unlimitedBucket;
        return signupBuckets.computeIfAbsent(clientIp, this::createSignupBucket);
    }

    private Bucket createStandardBucket(String clientIp) {
        Bandwidth limit = Bandwidth.classic(
                120,  // 버킷 용량 (최대 토큰)
                Refill.greedy(100, Duration.ofMinutes(1))  // 분당 100개 충전
        );
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createLoginBucket(String clientIp) {
        Bandwidth limit = Bandwidth.classic(
                15,  // 버킷 용량
                Refill.greedy(10, Duration.ofMinutes(1))  // 분당 10개 충전
        );
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createSignupBucket(String clientIp) {
        Bandwidth limit = Bandwidth.classic(
                10,  // 버킷 용량
                Refill.greedy(5, Duration.ofHours(1))  // 시간당 5개 충전
        );
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createUnlimitedBucket() {
        // 사실상 무제한: 분당 100만회 허용
        Bandwidth limit = Bandwidth.classic(
                1_000_000,
                Refill.greedy(1_000_000, Duration.ofMinutes(1))
        );
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Rate Limit 정책 정보 반환 (문서화용)
     */
    public Map<String, String> getRateLimitPolicies() {
        return Map.of(
                "standard", "100 requests/minute (burst: 120)",
                "login", "10 requests/minute (burst: 15)",
                "signup", "5 requests/hour (burst: 10)"
        );
    }
}
