package com.runningapp.service;

import com.runningapp.domain.*;
import com.runningapp.dto.plan.PlanResponse;
import com.runningapp.dto.plan.PlanWeekResponse;
import com.runningapp.dto.plan.UserPlanResponse;
import com.runningapp.exception.BadRequestException;
import com.runningapp.exception.NotFoundException;
import com.runningapp.repository.PlanWeekRepository;
import com.runningapp.repository.RunningActivityRepository;
import com.runningapp.repository.TrainingPlanRepository;
import com.runningapp.repository.UserPlanRepository;
import com.runningapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 트레이닝 플랜 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrainingPlanService {

    private final TrainingPlanRepository planRepository;
    private final PlanWeekRepository planWeekRepository;
    private final UserPlanRepository userPlanRepository;
    private final UserRepository userRepository;
    private final RunningActivityRepository activityRepository;

    /** 플랜 목록 조회 (목표별 필터) */
    public List<PlanResponse> getPlans(GoalType goalType, PlanDifficulty difficulty) {
        List<TrainingPlan> plans;
        if (goalType != null && difficulty != null) {
            plans = planRepository.findByGoalTypeAndDifficulty(goalType, difficulty);
        } else if (goalType != null) {
            plans = planRepository.findByGoalTypeOrderByDifficulty(goalType);
        } else {
            plans = planRepository.findAll();
        }
        return plans.stream().map(PlanResponse::from).toList();
    }

    /** 추천 플랜 (목표/레벨 기반) */
    public List<PlanResponse> getRecommendedPlans(Long userId, GoalType goalType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

        // 목표 미지정 시 모든 플랜
        List<TrainingPlan> plans = goalType != null
                ? planRepository.findByGoalTypeOrderByDifficulty(goalType)
                : planRepository.findAll();

        // 레벨 기반 난이도 매칭 (level 1-3: BEGINNER, 4-6: INTERMEDIATE, 7+: ADVANCED)
        PlanDifficulty recommendedDiff = getDifficultyByLevel(user.getLevel());

        return plans.stream()
                .filter(p -> p.getDifficulty() == recommendedDiff || p.getDifficulty().ordinal() <= recommendedDiff.ordinal())
                .limit(3)
                .map(PlanResponse::from)
                .toList();
    }

    private PlanDifficulty getDifficultyByLevel(int level) {
        if (level <= 3) return PlanDifficulty.BEGINNER;
        if (level <= 6) return PlanDifficulty.INTERMEDIATE;
        return PlanDifficulty.ADVANCED;
    }

    @Transactional
    public UserPlanResponse startPlan(Long userId, Long planId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));
        TrainingPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new NotFoundException("플랜을 찾을 수 없습니다"));

        if (userPlanRepository.existsByUserIdAndPlanIdAndCompletedAtIsNull(userId, planId)) {
            throw new BadRequestException("이미 진행중인 플랜입니다");
        }

        UserPlan userPlan = UserPlan.builder()
                .user(user)
                .plan(plan)
                .currentWeek(1)
                .build();
        userPlan = userPlanRepository.save(userPlan);

        return UserPlanResponse.from(userPlan);
    }

    /** 내 진행 플랜 목록 */
    public List<UserPlanResponse> getMyPlans(Long userId) {
        return userPlanRepository.findByUserIdOrderByStartedAtDesc(userId).stream()
                .map(UserPlanResponse::from)
                .toList();
    }

    /** 러닝 활동 저장 시 호출 - 진행중인 플랜 주차 진행 체크 */
    @Transactional
    public void updatePlanProgressOnActivity(Long userId, double distance, LocalDateTime activityStartedAt) {
        List<UserPlan> activePlans = userPlanRepository.findActiveByUserId(userId);
        LocalDate activityDate = activityStartedAt.toLocalDate();

        for (UserPlan userPlan : activePlans) {
            LocalDateTime planStart = userPlan.getStartedAt();
            int weekNum = (int) java.time.temporal.ChronoUnit.WEEKS.between(
                    planStart.toLocalDate(), activityDate) + 1;

            // 활동이 현재 주에 해당하는지
            if (weekNum != userPlan.getCurrentWeek()) continue;
            if (weekNum > userPlan.getPlan().getTotalWeeks()) continue;

            // 해당 주의 목표 조회
            List<PlanWeek> weeks = planWeekRepository.findByPlanIdOrderByWeekNumberAsc(userPlan.getPlan().getId());
            PlanWeek currentPlanWeek = weeks.stream()
                    .filter(w -> w.getWeekNumber() == weekNum)
                    .findFirst()
                    .orElse(null);
            if (currentPlanWeek == null) continue;

            // 해당 주의 누적 거리/횟수 계산
            LocalDateTime weekStart = planStart.plusWeeks(weekNum - 1);
            LocalDateTime weekEnd = weekStart.plusWeeks(1);
            Double weekDistance = activityRepository.sumDistanceByUserIdAndDateRange(userId, weekStart, weekEnd);
            long weekRunCount = activityRepository.countByUserIdAndDateRange(userId, weekStart, weekEnd);

            // 목표 달성 시 다음 주로
            Double targetDist = currentPlanWeek.getTargetDistance();
            Integer targetRuns = currentPlanWeek.getTargetRuns();
            boolean distanceOk = (targetDist == null || (weekDistance != null && weekDistance >= targetDist));
            boolean runsOk = (targetRuns == null || weekRunCount >= targetRuns);

            if (distanceOk && runsOk) {
                if (userPlan.getCurrentWeek() >= userPlan.getPlan().getTotalWeeks()) {
                    userPlan.complete();
                } else {
                    userPlan.advanceWeek();
                }
            }
        }
    }

    /** 주차별 스케줄 조회 */
    public List<PlanWeekResponse> getSchedule(Long planId) {
        TrainingPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new NotFoundException("플랜을 찾을 수 없습니다"));

        return planWeekRepository.findByPlanIdOrderByWeekNumberAsc(planId).stream()
                .map(PlanWeekResponse::from)
                .toList();
    }
}
