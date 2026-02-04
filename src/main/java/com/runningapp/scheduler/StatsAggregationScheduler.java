package com.runningapp.scheduler;

import com.runningapp.repository.RunningActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 통계 집계 스케줄러
 *
 * 매주 월요일 00:30에 주간 통계 집계 및 캐시 초기화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatsAggregationScheduler {

    private final RunningActivityRepository activityRepository;
    private final CacheManager cacheManager;

    @Scheduled(cron = "0 30 0 * * MON")  // 매주 월요일 00:30
    public void aggregateWeeklyStats() {
        log.info("주간 통계 집계 시작");

        // 지난 주 기간 계산
        LocalDate lastMonday = LocalDate.now().with(DayOfWeek.MONDAY).minusWeeks(1);
        LocalDateTime weekStart = lastMonday.atStartOfDay();
        LocalDateTime weekEnd = weekStart.plusWeeks(1);

        // 주간 통계 조회 (로그용)
        Long totalRuns = activityRepository.countByStartedAtBetween(weekStart, weekEnd);
        Double totalDistance = activityRepository.sumDistanceByDateRange(weekStart, weekEnd);

        log.info("지난 주 통계: period={} ~ {}, totalRuns={}, totalDistance={}km",
                weekStart.toLocalDate(), weekEnd.toLocalDate(), totalRuns, totalDistance);

        // 캐시 초기화 (activitySummary, activityStats)
        invalidateCache("activitySummary");
        invalidateCache("activityStats");

        log.info("주간 통계 집계 완료 + 캐시 초기화");
    }

    private void invalidateCache(String cacheName) {
        try {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                log.debug("캐시 초기화: {}", cacheName);
            }
        } catch (Exception e) {
            log.warn("캐시 초기화 실패: {}, error={}", cacheName, e.getMessage());
        }
    }
}
