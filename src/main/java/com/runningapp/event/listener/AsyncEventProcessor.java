package com.runningapp.event.listener;

import com.runningapp.domain.Challenge;
import com.runningapp.domain.ChallengeType;
import com.runningapp.domain.PlanWeek;
import com.runningapp.domain.User;
import com.runningapp.domain.UserChallenge;
import com.runningapp.domain.UserPlan;
import com.runningapp.repository.PlanWeekRepository;
import com.runningapp.repository.RunningActivityRepository;
import com.runningapp.repository.UserChallengeRepository;
import com.runningapp.repository.UserPlanRepository;
import com.runningapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 비동기 이벤트 처리기
 *
 * @Async가 제대로 프록시를 통해 호출되도록 별도 빈으로 분리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncEventProcessor {

    private final UserRepository userRepository;
    private final UserChallengeRepository userChallengeRepository;
    private final UserPlanRepository userPlanRepository;
    private final PlanWeekRepository planWeekRepository;
    private final RunningActivityRepository activityRepository;

    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void processLevelUpdate(Long userId, double distanceDelta, String action) {
        log.debug("레벨 업데이트 - 활동 {}: userId={}, distanceDelta={}", action, userId, distanceDelta);

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("사용자를 찾을 수 없음: userId={}", userId);
            return;
        }

        user.addDistance(distanceDelta);
        user.updateLevel();
        userRepository.save(user);

        log.info("레벨 업데이트 완료 ({}): userId={}, newLevel={}, totalDistance={}",
                action, user.getId(), user.getLevel(), user.getTotalDistance());
    }

    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void processChallengeProgress(Long userId, double distance, LocalDate activityDate) {
        log.debug("챌린지 진행률 업데이트: userId={}, distance={}", userId, distance);

        List<UserChallenge> activeUserChallenges = userChallengeRepository.findActiveByUserId(userId);

        for (UserChallenge uc : activeUserChallenges) {
            if (uc.isCompleted()) continue;
            if (!uc.getChallenge().isActive(activityDate)) continue;

            Challenge c = uc.getChallenge();
            if (c.getType() == ChallengeType.DISTANCE) {
                uc.addDistance(distance);
            } else {
                uc.addCount();
            }

            if (uc.isGoalAchieved()) {
                uc.complete();
                log.info("챌린지 완료: userId={}, challengeId={}", userId, c.getId());
            }
        }

        log.debug("챌린지 진행률 업데이트 완료: userId={}, updatedCount={}", userId, activeUserChallenges.size());
    }

    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void processPlanProgress(Long userId, double distance, LocalDateTime startedAt) {
        log.debug("플랜 진행 업데이트: userId={}, distance={}", userId, distance);

        List<UserPlan> activePlans = userPlanRepository.findActiveByUserId(userId);
        LocalDate activityDate = startedAt.toLocalDate();

        for (UserPlan userPlan : activePlans) {
            LocalDateTime planStart = userPlan.getStartedAt();
            int weekNum = (int) ChronoUnit.WEEKS.between(planStart.toLocalDate(), activityDate) + 1;

            if (weekNum != userPlan.getCurrentWeek()) continue;
            if (weekNum > userPlan.getPlan().getTotalWeeks()) continue;

            List<PlanWeek> weeks = planWeekRepository.findByPlanIdOrderByWeekNumberAsc(userPlan.getPlan().getId());
            PlanWeek currentPlanWeek = weeks.stream()
                    .filter(w -> w.getWeekNumber() == weekNum)
                    .findFirst()
                    .orElse(null);
            if (currentPlanWeek == null) continue;

            LocalDateTime weekStart = planStart.plusWeeks(weekNum - 1);
            LocalDateTime weekEnd = weekStart.plusWeeks(1);
            Double weekDistance = activityRepository.sumDistanceByUserIdAndDateRange(userId, weekStart, weekEnd);
            long weekRunCount = activityRepository.countByUserIdAndDateRange(userId, weekStart, weekEnd);

            Double targetDist = currentPlanWeek.getTargetDistance();
            Integer targetRuns = currentPlanWeek.getTargetRuns();
            boolean distanceOk = (targetDist == null || (weekDistance != null && weekDistance >= targetDist));
            boolean runsOk = (targetRuns == null || weekRunCount >= targetRuns);

            if (distanceOk && runsOk) {
                if (userPlan.getCurrentWeek() >= userPlan.getPlan().getTotalWeeks()) {
                    userPlan.complete();
                    log.info("플랜 완료: userId={}, planId={}", userId, userPlan.getPlan().getId());
                } else {
                    userPlan.advanceWeek();
                    log.info("플랜 주차 진행: userId={}, planId={}, newWeek={}",
                            userId, userPlan.getPlan().getId(), userPlan.getCurrentWeek());
                }
            }
        }
    }
}
