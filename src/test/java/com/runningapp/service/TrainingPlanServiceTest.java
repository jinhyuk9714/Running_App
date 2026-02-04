package com.runningapp.service;

import com.runningapp.domain.*;
import com.runningapp.dto.plan.PlanResponse;
import com.runningapp.dto.plan.PlanWeekResponse;
import com.runningapp.dto.plan.UserPlanResponse;
import com.runningapp.exception.BadRequestException;
import com.runningapp.exception.NotFoundException;
import com.runningapp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrainingPlanService 단위 테스트")
class TrainingPlanServiceTest {

    @Mock
    private TrainingPlanRepository planRepository;

    @Mock
    private PlanWeekRepository planWeekRepository;

    @Mock
    private UserPlanRepository userPlanRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RunningActivityRepository activityRepository;

    @InjectMocks
    private TrainingPlanService planService;

    private User testUser;
    private TrainingPlan testPlan;
    private UserPlan testUserPlan;
    private PlanWeek testPlanWeek;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@test.com")
                .password("encodedPassword")
                .nickname("테스터")
                .build();
        setField(testUser, "id", 1L);
        setField(testUser, "level", 3);

        testPlan = TrainingPlan.builder()
                .name("5K 초급 플랜")
                .description("5km 달리기를 위한 8주 프로그램")
                .goalType(GoalType.FIVE_K)
                .difficulty(PlanDifficulty.BEGINNER)
                .totalWeeks(8)
                .totalRuns(24)
                .build();
        setField(testPlan, "id", 1L);

        testUserPlan = UserPlan.builder()
                .user(testUser)
                .plan(testPlan)
                .currentWeek(1)
                .build();
        setField(testUserPlan, "id", 1L);

        testPlanWeek = PlanWeek.builder()
                .plan(testPlan)
                .weekNumber(1)
                .targetDistance(5.0)
                .targetRuns(3)
                .description("첫 주: 가볍게 시작")
                .build();
        setField(testPlanWeek, "id", 1L);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }

    @Nested
    @DisplayName("getPlans()")
    class GetPlans {

        @Test
        @DisplayName("성공 - 전체 플랜 목록 조회")
        void getPlans_all_success() {
            // given
            given(planRepository.findAll()).willReturn(List.of(testPlan));

            // when
            List<PlanResponse> result = planService.getPlans(null, null);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("5K 초급 플랜");
        }

        @Test
        @DisplayName("성공 - 목표별 필터 조회")
        void getPlans_byGoalType_success() {
            // given
            given(planRepository.findByGoalTypeOrderByDifficulty(GoalType.FIVE_K))
                    .willReturn(List.of(testPlan));

            // when
            List<PlanResponse> result = planService.getPlans(GoalType.FIVE_K, null);

            // then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("성공 - 목표+난이도 필터 조회")
        void getPlans_byGoalTypeAndDifficulty_success() {
            // given
            given(planRepository.findByGoalTypeAndDifficulty(GoalType.FIVE_K, PlanDifficulty.BEGINNER))
                    .willReturn(List.of(testPlan));

            // when
            List<PlanResponse> result = planService.getPlans(GoalType.FIVE_K, PlanDifficulty.BEGINNER);

            // then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getRecommendedPlans()")
    class GetRecommendedPlans {

        @Test
        @DisplayName("성공 - 추천 플랜 조회")
        void getRecommendedPlans_success() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(planRepository.findAll()).willReturn(List.of(testPlan));

            // when
            List<PlanResponse> result = planService.getRecommendedPlans(1L, null);

            // then
            assertThat(result).isNotEmpty();
        }

        @Test
        @DisplayName("성공 - 목표별 추천 플랜 조회")
        void getRecommendedPlans_byGoalType_success() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(planRepository.findByGoalTypeOrderByDifficulty(GoalType.FIVE_K))
                    .willReturn(List.of(testPlan));

            // when
            List<PlanResponse> result = planService.getRecommendedPlans(1L, GoalType.FIVE_K);

            // then
            assertThat(result).isNotEmpty();
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사용자")
        void getRecommendedPlans_userNotFound_throwsException() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> planService.getRecommendedPlans(999L, null))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("사용자를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("startPlan()")
    class StartPlan {

        @Test
        @DisplayName("성공 - 플랜 시작")
        void startPlan_success() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(planRepository.findById(1L)).willReturn(Optional.of(testPlan));
            given(userPlanRepository.existsByUserIdAndPlanIdAndCompletedAtIsNull(1L, 1L))
                    .willReturn(false);
            given(userPlanRepository.save(any(UserPlan.class))).willAnswer(invocation -> {
                UserPlan up = invocation.getArgument(0);
                setField(up, "id", 1L);
                return up;
            });

            // when
            UserPlanResponse response = planService.startPlan(1L, 1L);

            // then
            assertThat(response).isNotNull();
            verify(userPlanRepository).save(any(UserPlan.class));
        }

        @Test
        @DisplayName("실패 - 이미 진행 중인 플랜")
        void startPlan_alreadyInProgress_throwsException() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(planRepository.findById(1L)).willReturn(Optional.of(testPlan));
            given(userPlanRepository.existsByUserIdAndPlanIdAndCompletedAtIsNull(1L, 1L))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> planService.startPlan(1L, 1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("이미 진행중인 플랜입니다");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 플랜")
        void startPlan_planNotFound_throwsException() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(planRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> planService.startPlan(1L, 999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("플랜을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("getMyPlans()")
    class GetMyPlans {

        @Test
        @DisplayName("성공 - 내 플랜 목록 조회")
        void getMyPlans_success() {
            // given - JOIN FETCH 쿼리 사용
            given(userPlanRepository.findByUserIdWithPlan(1L))
                    .willReturn(List.of(testUserPlan));

            // when
            List<UserPlanResponse> result = planService.getMyPlans(1L);

            // then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getSchedule()")
    class GetSchedule {

        @Test
        @DisplayName("성공 - 플랜 스케줄 조회")
        void getSchedule_success() {
            // given
            given(planRepository.findById(1L)).willReturn(Optional.of(testPlan));
            given(planWeekRepository.findByPlanIdOrderByWeekNumberAsc(1L))
                    .willReturn(List.of(testPlanWeek));

            // when
            List<PlanWeekResponse> result = planService.getSchedule(1L);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getWeekNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 플랜")
        void getSchedule_planNotFound_throwsException() {
            // given
            given(planRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> planService.getSchedule(999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("플랜을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("updatePlanProgressOnActivity()")
    class UpdatePlanProgressOnActivity {

        @Test
        @DisplayName("성공 - 활동 없을 때 아무 일도 안함")
        void updatePlanProgressOnActivity_noActivePlan_success() {
            // given
            given(userPlanRepository.findActiveByUserId(1L)).willReturn(List.of());

            // when
            planService.updatePlanProgressOnActivity(1L, 5.0, java.time.LocalDateTime.now());

            // then
            verify(userPlanRepository).findActiveByUserId(1L);
            verifyNoInteractions(planWeekRepository);
        }
    }
}
