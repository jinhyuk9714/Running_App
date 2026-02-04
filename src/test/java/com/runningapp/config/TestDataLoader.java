package com.runningapp.config;

import com.runningapp.domain.*;
import com.runningapp.repository.ChallengeRepository;
import com.runningapp.repository.PlanWeekRepository;
import com.runningapp.repository.TrainingPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.LocalDate;

/**
 * 테스트용 시드 데이터 로더
 */
@Configuration
@Profile("test")
@RequiredArgsConstructor
public class TestDataLoader {

    private final ChallengeRepository challengeRepository;
    private final TrainingPlanRepository planRepository;
    private final PlanWeekRepository planWeekRepository;

    @Bean
    public ApplicationRunner testDataRunner() {
        return args -> {
            loadChallenges();
            loadPlans();
        };
    }

    private void loadChallenges() {
        if (challengeRepository.count() > 0) return;

        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

        // 거리 챌린지
        challengeRepository.save(Challenge.builder()
                .name("이번 달 50km 달리기")
                .description("초급자용! 한 달 동안 50km를 달성해보세요.")
                .targetDistance(50.0)
                .startDate(monthStart)
                .endDate(monthEnd)
                .type(ChallengeType.DISTANCE)
                .recommendedMinLevel(1)
                .build());

        challengeRepository.save(Challenge.builder()
                .name("이번 달 100km 달리기")
                .description("한 달 동안 100km를 달성해보세요!")
                .targetDistance(100.0)
                .startDate(monthStart)
                .endDate(monthEnd)
                .type(ChallengeType.DISTANCE)
                .recommendedMinLevel(3)
                .build());

        // 횟수 챌린지
        challengeRepository.save(Challenge.builder()
                .name("이번 달 8회 달리기")
                .description("초급자용! 한 달 동안 8번 러닝에 도전하세요.")
                .targetCount(8)
                .startDate(monthStart)
                .endDate(monthEnd)
                .type(ChallengeType.COUNT)
                .recommendedMinLevel(1)
                .build());
    }

    private void loadPlans() {
        if (planRepository.count() > 0) return;

        // 5K 완주 플랜 (BEGINNER)
        TrainingPlan plan5k = planRepository.save(TrainingPlan.builder()
                .name("5K 완주 플랜")
                .description("8주 동안 5km 완주를 목표로 하는 초급자용 플랜")
                .goalType(GoalType.FIVE_K)
                .difficulty(PlanDifficulty.BEGINNER)
                .totalWeeks(8)
                .totalRuns(24)
                .build());

        // 주차별 스케줄 추가
        for (int week = 1; week <= 8; week++) {
            planWeekRepository.save(PlanWeek.builder()
                    .plan(plan5k)
                    .weekNumber(week)
                    .targetDistance(2.0 + (week * 0.5))
                    .targetRuns(3)
                    .description(week + "주차 훈련")
                    .build());
        }

        // 10K 완주 플랜 (BEGINNER)
        TrainingPlan plan10k = planRepository.save(TrainingPlan.builder()
                .name("10K 완주 플랜")
                .description("10주 동안 10km 완주를 목표로 하는 플랜")
                .goalType(GoalType.TEN_K)
                .difficulty(PlanDifficulty.BEGINNER)
                .totalWeeks(10)
                .totalRuns(40)
                .build());

        for (int week = 1; week <= 10; week++) {
            planWeekRepository.save(PlanWeek.builder()
                    .plan(plan10k)
                    .weekNumber(week)
                    .targetDistance(5.0 + (week * 0.5))
                    .targetRuns(4)
                    .description(week + "주차 훈련")
                    .build());
        }
    }
}
