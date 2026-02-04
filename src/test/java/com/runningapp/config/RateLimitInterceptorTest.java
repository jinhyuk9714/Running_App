package com.runningapp.config;

import com.runningapp.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitInterceptorTest {

    @Mock
    private RateLimitConfig rateLimitConfig;

    @InjectMocks
    private RateLimitInterceptor rateLimitInterceptor;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.setRemoteAddr("127.0.0.1");
    }

    @Test
    @DisplayName("일반 API 요청 - Rate Limit 내에서 허용")
    void standardApi_withinLimit_shouldAllow() throws Exception {
        // given
        request.setRequestURI("/api/activities");
        request.setMethod("GET");

        Bucket bucket = createBucketWithTokens(100);
        when(rateLimitConfig.resolveBucket(anyString())).thenReturn(bucket);

        // when
        boolean result = rateLimitInterceptor.preHandle(request, response, null);

        // then
        assertThat(result).isTrue();
        assertThat(response.getHeader("X-Rate-Limit-Remaining")).isNotNull();
    }

    @Test
    @DisplayName("로그인 API - 전용 Rate Limit 버킷 사용")
    void loginApi_shouldUseLoginBucket() throws Exception {
        // given
        request.setRequestURI("/api/auth/login");
        request.setMethod("POST");

        Bucket bucket = createBucketWithTokens(10);
        when(rateLimitConfig.resolveLoginBucket(anyString())).thenReturn(bucket);

        // when
        boolean result = rateLimitInterceptor.preHandle(request, response, null);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("회원가입 API - 전용 Rate Limit 버킷 사용")
    void signupApi_shouldUseSignupBucket() throws Exception {
        // given
        request.setRequestURI("/api/auth/signup");
        request.setMethod("POST");

        Bucket bucket = createBucketWithTokens(5);
        when(rateLimitConfig.resolveSignupBucket(anyString())).thenReturn(bucket);

        // when
        boolean result = rateLimitInterceptor.preHandle(request, response, null);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Rate Limit 초과 - 429 에러 발생")
    void exceedRateLimit_shouldThrowException() {
        // given
        request.setRequestURI("/api/activities");
        request.setMethod("GET");

        Bucket bucket = createEmptyBucket();
        when(rateLimitConfig.resolveBucket(anyString())).thenReturn(bucket);

        // when & then
        assertThatThrownBy(() -> rateLimitInterceptor.preHandle(request, response, null))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("요청 한도를 초과했습니다");
    }

    @Test
    @DisplayName("X-Forwarded-For 헤더로 클라이언트 IP 추출")
    void shouldExtractClientIpFromXForwardedFor() throws Exception {
        // given
        request.setRequestURI("/api/activities");
        request.setMethod("GET");
        request.addHeader("X-Forwarded-For", "203.0.113.195, 70.41.3.18, 150.172.238.178");

        Bucket bucket = createBucketWithTokens(100);
        when(rateLimitConfig.resolveBucket("203.0.113.195")).thenReturn(bucket);

        // when
        boolean result = rateLimitInterceptor.preHandle(request, response, null);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("X-Real-IP 헤더로 클라이언트 IP 추출")
    void shouldExtractClientIpFromXRealIp() throws Exception {
        // given
        request.setRequestURI("/api/activities");
        request.setMethod("GET");
        request.addHeader("X-Real-IP", "192.168.1.100");

        Bucket bucket = createBucketWithTokens(100);
        when(rateLimitConfig.resolveBucket("192.168.1.100")).thenReturn(bucket);

        // when
        boolean result = rateLimitInterceptor.preHandle(request, response, null);

        // then
        assertThat(result).isTrue();
    }

    private Bucket createBucketWithTokens(long tokens) {
        Bandwidth limit = Bandwidth.classic(tokens,
                io.github.bucket4j.Refill.greedy(100, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createEmptyBucket() {
        // 용량 1, 리필 없는 버킷 생성 후 토큰 소비
        Bandwidth limit = Bandwidth.classic(1,
                io.github.bucket4j.Refill.intervally(1, Duration.ofHours(1)));
        Bucket bucket = Bucket.builder().addLimit(limit).build();
        bucket.tryConsume(1);  // 토큰 소비하여 비움
        return bucket;
    }
}
