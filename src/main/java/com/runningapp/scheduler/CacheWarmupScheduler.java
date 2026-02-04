package com.runningapp.scheduler;

import com.runningapp.service.ChallengeService;
import com.runningapp.service.TrainingPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 캐시 워밍업 스케줄러
 *
 * 5분마다 activeChallenges, plans 캐시 워밍업
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheWarmupScheduler {

    private final ChallengeService challengeService;
    private final TrainingPlanService trainingPlanService;

    @Scheduled(fixedRate = 300_000)  // 5분마다
    public void warmupCaches() {
        log.debug("캐시 워밍업 시작");

        try {
            // 진행중인 챌린지 목록 캐시
            challengeService.getActiveChallenges();
            log.debug("activeChallenges 캐시 워밍업 완료");
        } catch (Exception e) {
            log.warn("activeChallenges 캐시 워밍업 실패: {}", e.getMessage());
        }

        try {
            // 플랜 목록 캐시 (전체)
            trainingPlanService.getPlans(null, null);
            log.debug("plans 캐시 워밍업 완료");
        } catch (Exception e) {
            log.warn("plans 캐시 워밍업 실패: {}", e.getMessage());
        }

        log.debug("캐시 워밍업 완료");
    }
}
