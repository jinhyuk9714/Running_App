package com.runningapp.config;

import com.runningapp.exception.RateLimitExceededException;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Rate Limiting 인터셉터
 *
 * 엔드포인트별 차등 제한:
 * - POST /api/auth/login: 분당 10회 (브루트포스 방지)
 * - POST /api/auth/signup: 시간당 5회 (스팸 방지)
 * - 그 외 API: 분당 100회
 *
 * 응답 헤더:
 * - X-Rate-Limit-Remaining: 남은 요청 수
 * - X-Rate-Limit-Retry-After-Seconds: 재시도까지 대기 시간 (제한 초과 시)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitConfig rateLimitConfig;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String clientIp = getClientIp(request);
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Rate Limit 적용할 버킷 선택
        Bucket bucket = selectBucket(clientIp, path, method);

        // 토큰 소비 시도
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        // 남은 요청 수 헤더 추가
        response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));

        if (probe.isConsumed()) {
            return true;  // 요청 허용
        } else {
            // Rate Limit 초과
            long waitTimeSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitTimeSeconds));

            log.warn("Rate limit exceeded for IP: {}, path: {}, retry after: {}s",
                    clientIp, path, waitTimeSeconds);

            throw new RateLimitExceededException(
                    "요청 한도를 초과했습니다. " + waitTimeSeconds + "초 후에 다시 시도해주세요."
            );
        }
    }

    /**
     * 엔드포인트별 버킷 선택
     */
    private Bucket selectBucket(String clientIp, String path, String method) {
        if ("POST".equals(method)) {
            if (path.equals("/api/auth/login")) {
                return rateLimitConfig.resolveLoginBucket(clientIp);
            } else if (path.equals("/api/auth/signup")) {
                return rateLimitConfig.resolveSignupBucket(clientIp);
            }
        }
        return rateLimitConfig.resolveBucket(clientIp);
    }

    /**
     * 클라이언트 IP 추출 (프록시 환경 고려)
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // 첫 번째 IP가 실제 클라이언트 IP
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
