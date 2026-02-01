package com.runningapp.config;

import com.runningapp.domain.*;
import com.runningapp.repository.PlanWeekRepository;
import com.runningapp.repository.TrainingPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 트레이닝 플랜 시드 데이터 로더
 *
 * 5K, 10K, 하프마라톤 목표별 플랜 생성
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class PlanDataLoader implements CommandLineRunner {

    private final TrainingPlanRepository planRepository;
    private final PlanWeekRepository planWeekRepository;

    @Override
    public void run(String... args) {
        if (planRepository.count() > 0) {
            return;
        }

        // BEGINNER
        TrainingPlan plan5k = create5KPlan();
        TrainingPlan plan10k = create10KPlan();
        TrainingPlan planHalf = createHalfMarathonPlan();
        // INTERMEDIATE
        TrainingPlan plan5kMid = create5KPlanIntermediate();
        TrainingPlan plan10kMid = create10KPlanIntermediate();
        TrainingPlan planHalfMid = createHalfMarathonPlanIntermediate();
        // ADVANCED
        TrainingPlan plan5kAdv = create5KPlanAdvanced();
        TrainingPlan plan10kAdv = create10KPlanAdvanced();
        TrainingPlan planHalfAdv = createHalfMarathonPlanAdvanced();

        planRepository.save(plan5k);
        planRepository.save(plan10k);
        planRepository.save(planHalf);
        planRepository.save(plan5kMid);
        planRepository.save(plan10kMid);
        planRepository.save(planHalfMid);
        planRepository.save(plan5kAdv);
        planRepository.save(plan10kAdv);
        planRepository.save(planHalfAdv);

        // PlanWeek 추가 (5K 초급)
        for (int w = 1; w <= 8; w++) {
            double dist = 8 + w * 1.5;
            planWeekRepository.save(createPlanWeek(plan5k, w, dist, 3));
        }

        // PlanWeek 추가 (10K 초급)
        for (int w = 1; w <= 10; w++) {
            double dist = 12 + w * 2.0;
            planWeekRepository.save(createPlanWeek(plan10k, w, dist, 4));
        }

        // PlanWeek 추가 (하프마라톤 초급)
        for (int w = 1; w <= 12; w++) {
            double dist = 15 + w * 2.5;
            planWeekRepository.save(createPlanWeek(planHalf, w, dist, 4));
        }

        // PlanWeek 추가 (5K 중급 - 6주)
        for (int w = 1; w <= 6; w++) {
            double dist = 12 + w * 2.0;
            planWeekRepository.save(createPlanWeek(plan5kMid, w, dist, 4));
        }

        // PlanWeek 추가 (10K 중급 - 8주)
        for (int w = 1; w <= 8; w++) {
            double dist = 15 + w * 2.5;
            planWeekRepository.save(createPlanWeek(plan10kMid, w, dist, 4));
        }

        // PlanWeek 추가 (하프마라톤 중급 - 10주)
        for (int w = 1; w <= 10; w++) {
            double dist = 18 + w * 3.0;
            planWeekRepository.save(createPlanWeek(planHalfMid, w, dist, 5));
        }

        // PlanWeek 추가 (5K 고급 - 4주)
        for (int w = 1; w <= 4; w++) {
            double dist = 15 + w * 2.5;
            planWeekRepository.save(createPlanWeek(plan5kAdv, w, dist, 5));
        }

        // PlanWeek 추가 (10K 고급 - 6주)
        for (int w = 1; w <= 6; w++) {
            double dist = 18 + w * 3.0;
            planWeekRepository.save(createPlanWeek(plan10kAdv, w, dist, 5));
        }

        // PlanWeek 추가 (하프마라톤 고급 - 8주)
        for (int w = 1; w <= 8; w++) {
            double dist = 22 + w * 3.5;
            planWeekRepository.save(createPlanWeek(planHalfAdv, w, dist, 5));
        }

        log.info("트레이닝 플랜 시드 데이터 로드 완료: {}개", 9);
    }

    private PlanWeek createPlanWeek(TrainingPlan plan, int weekNum, double dist, int runs) {
        return PlanWeek.builder()
                .plan(plan)
                .weekNumber(weekNum)
                .targetDistance(dist)
                .targetRuns(runs)
                .description(weekNum + "주차: 주 " + runs + "회 러닝, 총 " + dist + "km")
                .build();
    }

    private TrainingPlan create5KPlan() {
        return TrainingPlan.builder()
                .name("5K 완주 플랜")
                .description("8주 동안 5km 완주를 목표로 하는 초급자용 플랜")
                .goalType(GoalType.FIVE_K)
                .difficulty(PlanDifficulty.BEGINNER)
                .totalWeeks(8)
                .totalRuns(24)
                .build();
    }

    private TrainingPlan create10KPlan() {
        return TrainingPlan.builder()
                .name("10K 완주 플랜")
                .description("10주 동안 10km 완주를 목표로 하는 플랜")
                .goalType(GoalType.TEN_K)
                .difficulty(PlanDifficulty.BEGINNER)
                .totalWeeks(10)
                .totalRuns(40)
                .build();
    }

    private TrainingPlan createHalfMarathonPlan() {
        return TrainingPlan.builder()
                .name("하프마라톤 완주 플랜")
                .description("12주 동안 21.0975km(하프마라톤) 완주를 목표로 하는 초급자용 플랜")
                .goalType(GoalType.HALF_MARATHON)
                .difficulty(PlanDifficulty.BEGINNER)
                .totalWeeks(12)
                .totalRuns(48)
                .build();
    }

    private TrainingPlan create5KPlanIntermediate() {
        return TrainingPlan.builder()
                .name("5K 속도업 플랜 (중급)")
                .description("6주 동안 5km 기록 단축을 목표로 하는 중급자용 플랜")
                .goalType(GoalType.FIVE_K)
                .difficulty(PlanDifficulty.INTERMEDIATE)
                .totalWeeks(6)
                .totalRuns(24)
                .build();
    }

    private TrainingPlan create10KPlanIntermediate() {
        return TrainingPlan.builder()
                .name("10K 속도업 플랜 (중급)")
                .description("8주 동안 10km 기록 단축을 목표로 하는 중급자용 플랜")
                .goalType(GoalType.TEN_K)
                .difficulty(PlanDifficulty.INTERMEDIATE)
                .totalWeeks(8)
                .totalRuns(32)
                .build();
    }

    private TrainingPlan createHalfMarathonPlanIntermediate() {
        return TrainingPlan.builder()
                .name("하프마라톤 기록단축 플랜 (중급)")
                .description("10주 동안 하프마라톤 기록 단축을 목표로 하는 중급자용 플랜")
                .goalType(GoalType.HALF_MARATHON)
                .difficulty(PlanDifficulty.INTERMEDIATE)
                .totalWeeks(10)
                .totalRuns(50)
                .build();
    }

    private TrainingPlan create5KPlanAdvanced() {
        return TrainingPlan.builder()
                .name("5K 챌린지 플랜 (고급)")
                .description("4주 동안 5km 최고 기록에 도전하는 고급자용 플랜")
                .goalType(GoalType.FIVE_K)
                .difficulty(PlanDifficulty.ADVANCED)
                .totalWeeks(4)
                .totalRuns(20)
                .build();
    }

    private TrainingPlan create10KPlanAdvanced() {
        return TrainingPlan.builder()
                .name("10K 챌린지 플랜 (고급)")
                .description("6주 동안 10km 최고 기록에 도전하는 고급자용 플랜")
                .goalType(GoalType.TEN_K)
                .difficulty(PlanDifficulty.ADVANCED)
                .totalWeeks(6)
                .totalRuns(30)
                .build();
    }

    private TrainingPlan createHalfMarathonPlanAdvanced() {
        return TrainingPlan.builder()
                .name("하프마라톤 챌린지 플랜 (고급)")
                .description("8주 동안 하프마라톤 최고 기록에 도전하는 고급자용 플랜")
                .goalType(GoalType.HALF_MARATHON)
                .difficulty(PlanDifficulty.ADVANCED)
                .totalWeeks(8)
                .totalRuns(40)
                .build();
    }
}
