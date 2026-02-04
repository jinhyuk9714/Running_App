package com.runningapp.service;

import com.runningapp.domain.RunningActivity;
import com.runningapp.domain.User;
import com.runningapp.dto.activity.ActivityRequest;
import com.runningapp.dto.activity.ActivityResponse;
import com.runningapp.dto.activity.ActivityStatsResponse;
import com.runningapp.dto.activity.ActivitySummaryResponse;
import com.runningapp.dto.activity.PeriodSummary;
import com.runningapp.exception.NotFoundException;
import com.runningapp.repository.RunningActivityRepository;
import com.runningapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 러닝 활동 서비스 (CRUD, 통계)
 *
 * 트랜잭션 전파: create/update/delete는 쓰기 작업이므로 @Transactional
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RunningActivityService {

    private final RunningActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final ChallengeService challengeService;
    private final TrainingPlanService trainingPlanService;

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "activitySummary", key = "#userId"),
            @CacheEvict(value = "activityStats", key = "#userId + '_' + T(java.time.LocalDate).now().year + '_' + T(java.time.LocalDate).now().monthValue")
    })
    public ActivityResponse create(Long userId, ActivityRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

        RunningActivity activity = RunningActivity.builder()
                .user(user)
                .distance(request.getDistance())
                .duration(request.getDuration())
                .averagePace(request.getAveragePace())
                .calories(request.getCalories())
                .averageHeartRate(request.getAverageHeartRate())
                .cadence(request.getCadence())
                .route(request.getRoute())
                .startedAt(request.getStartedAt())
                .memo(request.getMemo())
                .build();

        activity = activityRepository.save(activity);

        // 사용자 누적 거리 업데이트 + 레벨 재계산
        user.addDistance(request.getDistance());
        user.updateLevel();
        userRepository.save(user);

        // 챌린지 진행률 업데이트 (참여중인 챌린지)
        challengeService.updateProgressOnActivity(userId, request.getDistance(), request.getStartedAt().toLocalDate());

        // 플랜 주차 진행 체크 (진행중인 플랜)
        trainingPlanService.updatePlanProgressOnActivity(userId, request.getDistance(), request.getStartedAt());

        return ActivityResponse.from(activity);
    }

    public Page<ActivityResponse> getMyActivities(Long userId, Pageable pageable) {
        // sort 파라미터 오염 방지 (Swagger 등에서 sort=["string"] 전송 시 PropertyReferenceException 발생)
        Pageable safePageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "startedAt")
        );
        return activityRepository.findByUserIdOrderByStartedAtDesc(userId, safePageable)
                .map(ActivityResponse::from);
    }

    public ActivityResponse getActivity(Long userId, Long activityId) {
        RunningActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new NotFoundException("활동을 찾을 수 없습니다"));

        // 본인 활동만 조회 가능 (권한 체크)
        if (!activity.getUser().getId().equals(userId)) {
            throw new NotFoundException("활동을 찾을 수 없습니다");
        }

        return ActivityResponse.from(activity);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "activitySummary", key = "#userId"),
            @CacheEvict(value = "activityStats", key = "#userId + '_' + T(java.time.LocalDate).now().year + '_' + T(java.time.LocalDate).now().monthValue")
    })
    public ActivityResponse update(Long userId, Long activityId, ActivityRequest request) {
        RunningActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new NotFoundException("활동을 찾을 수 없습니다"));

        if (!activity.getUser().getId().equals(userId)) {
            throw new NotFoundException("활동을 찾을 수 없습니다");
        }

        // 거리 변경분 반영 (totalDistance 보정)
        double oldDistance = activity.getDistance();
        double newDistance = request.getDistance();

        activity.update(
                request.getDistance(),
                request.getDuration(),
                request.getAveragePace(),
                request.getCalories(),
                request.getAverageHeartRate(),
                request.getCadence(),
                request.getRoute(),
                request.getStartedAt(),
                request.getMemo()
        );

        User user = activity.getUser();
        user.addDistance(-oldDistance);
        user.addDistance(newDistance);
        user.updateLevel();
        userRepository.save(user);

        return ActivityResponse.from(activity);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "activitySummary", key = "#userId"),
            @CacheEvict(value = "activityStats", key = "#userId + '_' + T(java.time.LocalDate).now().year + '_' + T(java.time.LocalDate).now().monthValue")
    })
    public void delete(Long userId, Long activityId) {
        RunningActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new NotFoundException("활동을 찾을 수 없습니다"));

        if (!activity.getUser().getId().equals(userId)) {
            throw new NotFoundException("활동을 찾을 수 없습니다");
        }

        User user = activity.getUser();
        user.addDistance(-activity.getDistance());
        user.updateLevel();
        userRepository.save(user);

        activityRepository.delete(activity);
    }

    @Cacheable(value = "activityStats", key = "#userId + '_' + #year + '_' + #month", condition = "#year != null && #month != null")
    public ActivityStatsResponse getStats(Long userId, Integer year, Integer month) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

        LocalDateTime start;
        LocalDateTime end;

        // year, month에 따라 조회 기간 설정
        if (year != null && month != null) {
            start = LocalDateTime.of(year, month, 1, 0, 0);
            end = start.plusMonths(1);
        } else if (year != null) {
            start = LocalDateTime.of(year, 1, 1, 0, 0);
            end = start.plusYears(1);
        } else {
            return ActivityStatsResponse.builder()
                    .totalDistance(user.getTotalDistance())
                    .totalCount(activityRepository.findByUserIdOrderByStartedAtDesc(userId, Pageable.unpaged()).getNumberOfElements())
                    .totalDuration(null)
                    .averagePace(null)
                    .build();
        }

        Double totalDistance = activityRepository.sumDistanceByUserIdAndDateRange(userId, start, end);
        List<RunningActivity> activities = activityRepository.findByUserIdOrderByStartedAtDesc(
                userId, Pageable.unpaged()).getContent();

        List<RunningActivity> filtered = activities.stream()
                .filter(a -> !a.getStartedAt().isBefore(start) && a.getStartedAt().isBefore(end))
                .toList();

        int totalDuration = filtered.stream()
                .mapToInt(RunningActivity::getDuration)
                .sum();

        Integer avgPace = filtered.isEmpty() ? null :
                (int) filtered.stream()
                        .filter(a -> a.getAveragePace() != null)
                        .mapToInt(RunningActivity::getAveragePace)
                        .average()
                        .orElse(0);

        return ActivityStatsResponse.builder()
                .totalDistance(totalDistance != null ? totalDistance : 0.0)
                .totalCount(filtered.size())
                .totalDuration(totalDuration)
                .averagePace(avgPace)
                .build();
    }

    /** 주간/월간 요약: 이번 주, 이번 달, 지난달 통계 (ISO 주: 월요일 시작) */
    @Cacheable(value = "activitySummary", key = "#userId")
    public ActivitySummaryResponse getSummary(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

        LocalDate today = LocalDate.now();

        // 이번 주 (월요일 00:00 ~ 다음 월요일 00:00)
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusWeeks(1);
        PeriodSummary thisWeek = toPeriodSummary(
                userId,
                weekStart.atStartOfDay(),
                weekEnd.atStartOfDay()
        );

        // 이번 달
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = monthStart.plusMonths(1);
        PeriodSummary thisMonth = toPeriodSummary(
                userId,
                monthStart.atStartOfDay(),
                monthEnd.atStartOfDay()
        );

        // 지난달
        LocalDate lastMonthStart = monthStart.minusMonths(1);
        LocalDate lastMonthEnd = monthStart;
        PeriodSummary lastMonth = toPeriodSummary(
                userId,
                lastMonthStart.atStartOfDay(),
                lastMonthEnd.atStartOfDay()
        );

        return ActivitySummaryResponse.builder()
                .thisWeek(thisWeek)
                .thisMonth(thisMonth)
                .lastMonth(lastMonth)
                .build();
    }

    private PeriodSummary toPeriodSummary(Long userId, LocalDateTime start, LocalDateTime end) {
        Double totalDistance = activityRepository.sumDistanceByUserIdAndDateRange(userId, start, end);
        long count = activityRepository.countByUserIdAndDateRange(userId, start, end);
        List<RunningActivity> list = activityRepository.findByUserIdAndStartedAtBetween(userId, start, end);

        int totalDuration = list.stream().mapToInt(RunningActivity::getDuration).sum();
        Integer avgPace = list.isEmpty() ? null
                : (int) list.stream()
                        .filter(a -> a.getAveragePace() != null)
                        .mapToInt(RunningActivity::getAveragePace)
                        .average()
                        .orElse(0);

        return PeriodSummary.builder()
                .totalDistance(totalDistance != null ? totalDistance : 0.0)
                .totalCount((int) count)
                .totalDuration(totalDuration)
                .averagePace(avgPace)
                .build();
    }
}
