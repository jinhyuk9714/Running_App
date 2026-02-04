package com.runningapp.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * 비즈니스 메트릭 수집
 *
 * 카운터 (Counter):
 * - runningapp_users_signup_total: 총 회원가입 수
 * - runningapp_activities_created_total: 총 활동 생성 수
 * - runningapp_challenges_joined_total: 총 챌린지 참여 수
 *
 * 게이지 (Gauge):
 * - runningapp_active_users: 현재 활성 사용자 수 (추정)
 *
 * 타이머 (Timer):
 * - runningapp_api_duration: API별 응답 시간
 */
@Slf4j
@Component
public class BusinessMetrics {

    private final MeterRegistry registry;

    // Counters
    private final Counter signupCounter;
    private final Counter loginCounter;
    private final Counter loginFailCounter;
    private final Counter activityCreatedCounter;
    private final Counter challengeJoinedCounter;
    private final Counter planStartedCounter;

    // Gauges
    private final AtomicInteger activeUsers = new AtomicInteger(0);

    // Timers
    private final Timer activityCreateTimer;

    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;

        // 회원가입 카운터
        this.signupCounter = Counter.builder("runningapp_users_signup")
                .description("Total number of user signups")
                .tag("type", "signup")
                .register(registry);

        // 로그인 카운터
        this.loginCounter = Counter.builder("runningapp_auth_login")
                .description("Total number of successful logins")
                .tag("status", "success")
                .register(registry);

        this.loginFailCounter = Counter.builder("runningapp_auth_login")
                .description("Total number of failed logins")
                .tag("status", "failure")
                .register(registry);

        // 활동 생성 카운터
        this.activityCreatedCounter = Counter.builder("runningapp_activities_created")
                .description("Total number of running activities created")
                .register(registry);

        // 챌린지 참여 카운터
        this.challengeJoinedCounter = Counter.builder("runningapp_challenges_joined")
                .description("Total number of challenge participations")
                .register(registry);

        // 플랜 시작 카운터
        this.planStartedCounter = Counter.builder("runningapp_plans_started")
                .description("Total number of training plans started")
                .register(registry);

        // 활성 사용자 게이지 (예시)
        Gauge.builder("runningapp_active_users", activeUsers, AtomicInteger::get)
                .description("Estimated number of active users")
                .register(registry);

        // 활동 생성 타이머
        this.activityCreateTimer = Timer.builder("runningapp_activity_create_duration")
                .description("Time taken to create an activity")
                .register(registry);
    }

    // ========== Counter Increments ==========

    public void incrementSignup() {
        signupCounter.increment();
        log.debug("Metric: signup incremented");
    }

    public void incrementLoginSuccess() {
        loginCounter.increment();
        activeUsers.incrementAndGet();
    }

    public void incrementLoginFailure() {
        loginFailCounter.increment();
    }

    public void incrementActivityCreated() {
        activityCreatedCounter.increment();
    }

    public void incrementChallengeJoined() {
        challengeJoinedCounter.increment();
    }

    public void incrementPlanStarted() {
        planStartedCounter.increment();
    }

    // ========== Timer ==========

    public Timer.Sample startActivityTimer() {
        return Timer.start(registry);
    }

    public void stopActivityTimer(Timer.Sample sample) {
        sample.stop(activityCreateTimer);
    }

    // ========== Gauge Updates ==========

    public void setActiveUsers(int count) {
        activeUsers.set(count);
    }

    public void decrementActiveUsers() {
        activeUsers.decrementAndGet();
    }
}
