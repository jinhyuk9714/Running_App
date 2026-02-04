package com.runningapp.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Prometheus 메트릭 설정
 *
 * 제공 메트릭:
 * - JVM: 메모리, GC, 스레드
 * - HTTP: 요청 수, 응답 시간, 에러율
 * - 커스텀: API별 응답 시간, 비즈니스 메트릭
 *
 * Prometheus 엔드포인트: GET /actuator/prometheus
 * Grafana 연동 시 해당 엔드포인트를 스크래핑 타겟으로 설정
 */
@Configuration
public class MetricsConfig {

    /**
     * 공통 태그 설정
     * - 모든 메트릭에 application, instance 태그 추가
     * - Grafana에서 앱별, 인스턴스별 필터링 가능
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> commonTags() {
        return registry -> registry.config()
                .commonTags(
                        "application", "running-app",
                        "framework", "spring-boot"
                );
    }

    /**
     * @Timed 어노테이션 지원
     * - 메서드에 @Timed 붙이면 자동으로 실행 시간 측정
     * - 예: @Timed(value = "auth.login", description = "로그인 처리 시간")
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}
