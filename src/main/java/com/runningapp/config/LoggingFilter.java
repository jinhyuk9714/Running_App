package com.runningapp.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 로깅 MDC 필터
 *
 * 모든 HTTP 요청에 대해 추적 정보를 MDC에 설정:
 * - requestId: 요청별 고유 ID (UUID)
 * - userId: 인증된 사용자 ID
 * - clientIp: 클라이언트 IP (프록시 환경 지원)
 * - method: HTTP 메서드
 * - uri: 요청 URI
 * - duration: 요청 처리 시간 (ms)
 *
 * MDC 데이터는 모든 로그 메시지에 자동 포함됨
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID = "requestId";
    private static final String USER_ID = "userId";
    private static final String CLIENT_IP = "clientIp";
    private static final String METHOD = "method";
    private static final String URI = "uri";
    private static final String DURATION = "duration";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();

        try {
            // MDC에 요청 정보 설정
            setupMDC(request);

            // 요청 시작 로그
            if (shouldLogRequest(request)) {
                log.info("Request started: {} {}", request.getMethod(), request.getRequestURI());
            }

            // 다음 필터 실행
            filterChain.doFilter(request, response);

        } finally {
            // 처리 시간 계산
            long duration = System.currentTimeMillis() - startTime;
            MDC.put(DURATION, String.valueOf(duration));

            // 요청 완료 로그
            if (shouldLogRequest(request)) {
                log.info("Request completed: {} {} - {} ({}ms)",
                        request.getMethod(),
                        request.getRequestURI(),
                        response.getStatus(),
                        duration);
            }

            // MDC 정리 (메모리 누수 방지)
            clearMDC();
        }
    }

    /**
     * MDC에 요청 추적 정보 설정
     */
    private void setupMDC(HttpServletRequest request) {
        // Request ID 생성 (기존 헤더가 있으면 사용)
        String requestId = request.getHeader("X-Request-ID");
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString().substring(0, 8);
        }
        MDC.put(REQUEST_ID, requestId);

        // 클라이언트 IP
        MDC.put(CLIENT_IP, getClientIp(request));

        // HTTP 메서드 및 URI
        MDC.put(METHOD, request.getMethod());
        MDC.put(URI, request.getRequestURI());

        // 사용자 ID (인증된 경우)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Long) {
            MDC.put(USER_ID, String.valueOf(auth.getPrincipal()));
        }
    }

    /**
     * MDC 정리
     */
    private void clearMDC() {
        MDC.remove(REQUEST_ID);
        MDC.remove(USER_ID);
        MDC.remove(CLIENT_IP);
        MDC.remove(METHOD);
        MDC.remove(URI);
        MDC.remove(DURATION);
    }

    /**
     * 클라이언트 IP 추출 (프록시 환경 지원)
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * 로깅 대상 요청 필터링
     * - Actuator, 정적 리소스 등 제외
     */
    private boolean shouldLogRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/api/") &&
                !uri.contains("/actuator/") &&
                !uri.contains("/swagger") &&
                !uri.contains("/api-docs");
    }
}
