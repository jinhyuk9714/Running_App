package com.runningapp.service;

import com.runningapp.domain.RunningActivity;
import com.runningapp.domain.User;
import com.runningapp.dto.activity.ActivityRequest;
import com.runningapp.dto.activity.ActivityResponse;
import com.runningapp.dto.activity.ActivityStatsResponse;
import com.runningapp.dto.activity.ActivitySummaryResponse;
import com.runningapp.exception.NotFoundException;
import com.runningapp.repository.RunningActivityRepository;
import com.runningapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RunningActivityService 단위 테스트")
class RunningActivityServiceTest {

    @Mock
    private RunningActivityRepository activityRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChallengeService challengeService;

    @Mock
    private TrainingPlanService trainingPlanService;

    @InjectMocks
    private RunningActivityService activityService;

    private User testUser;
    private RunningActivity testActivity;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@test.com")
                .password("encodedPassword")
                .nickname("테스터")
                .build();
        setField(testUser, "id", 1L);

        testActivity = RunningActivity.builder()
                .user(testUser)
                .distance(5.0)
                .duration(1800)
                .averagePace(360)
                .calories(300)
                .startedAt(LocalDateTime.of(2025, 2, 1, 7, 0))
                .build();
        setField(testActivity, "id", 1L);
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

    private ActivityRequest createActivityRequest(double distance, int duration, Integer avgPace,
                                                   Integer calories, LocalDateTime startedAt, String memo) {
        ActivityRequest request = new ActivityRequest();
        setField(request, "distance", distance);
        setField(request, "duration", duration);
        if (avgPace != null) setField(request, "averagePace", avgPace);
        if (calories != null) setField(request, "calories", calories);
        setField(request, "startedAt", startedAt);
        if (memo != null) setField(request, "memo", memo);
        return request;
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("성공 - 활동 생성")
        void create_success() {
            // given
            ActivityRequest request = createActivityRequest(5.0, 1800, 360, 300,
                    LocalDateTime.of(2025, 2, 1, 7, 0), "아침 러닝");

            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(activityRepository.save(any(RunningActivity.class))).willAnswer(invocation -> {
                RunningActivity activity = invocation.getArgument(0);
                setField(activity, "id", 1L);
                return activity;
            });
            given(userRepository.save(any(User.class))).willReturn(testUser);
            doNothing().when(challengeService).updateProgressOnActivity(anyLong(), anyDouble(), any());
            doNothing().when(trainingPlanService).updatePlanProgressOnActivity(anyLong(), anyDouble(), any());

            // when
            ActivityResponse response = activityService.create(1L, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getDistance()).isEqualTo(5.0);
            assertThat(response.getDuration()).isEqualTo(1800);

            verify(userRepository).findById(1L);
            verify(activityRepository).save(any(RunningActivity.class));
            verify(userRepository).save(testUser);
            verify(challengeService).updateProgressOnActivity(eq(1L), eq(5.0), any());
            verify(trainingPlanService).updatePlanProgressOnActivity(eq(1L), eq(5.0), any());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사용자")
        void create_userNotFound_throwsException() {
            // given
            ActivityRequest request = createActivityRequest(5.0, 1800, null, null,
                    LocalDateTime.now(), null);

            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> activityService.create(999L, request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("사용자를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("getMyActivities()")
    class GetMyActivities {

        @Test
        @DisplayName("성공 - 활동 목록 조회")
        void getMyActivities_success() {
            // given
            Page<RunningActivity> page = new PageImpl<>(List.of(testActivity));
            given(activityRepository.findByUserIdOrderByStartedAtDesc(eq(1L), any(Pageable.class)))
                    .willReturn(page);

            // when
            Page<ActivityResponse> result = activityService.getMyActivities(1L, PageRequest.of(0, 10));

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getDistance()).isEqualTo(5.0);
        }
    }

    @Nested
    @DisplayName("getActivity()")
    class GetActivity {

        @Test
        @DisplayName("성공 - 활동 상세 조회")
        void getActivity_success() {
            // given
            given(activityRepository.findById(1L)).willReturn(Optional.of(testActivity));

            // when
            ActivityResponse response = activityService.getActivity(1L, 1L);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getDistance()).isEqualTo(5.0);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 활동")
        void getActivity_notFound_throwsException() {
            // given
            given(activityRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> activityService.getActivity(1L, 999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("활동을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("실패 - 다른 사용자의 활동 조회")
        void getActivity_otherUserActivity_throwsException() {
            // given
            User otherUser = User.builder().email("other@test.com").password("pw").nickname("other").build();
            setField(otherUser, "id", 2L);

            RunningActivity otherActivity = RunningActivity.builder()
                    .user(otherUser)
                    .distance(3.0)
                    .duration(1000)
                    .startedAt(LocalDateTime.now())
                    .build();
            setField(otherActivity, "id", 2L);

            given(activityRepository.findById(2L)).willReturn(Optional.of(otherActivity));

            // when & then
            assertThatThrownBy(() -> activityService.getActivity(1L, 2L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("활동을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("성공 - 활동 수정")
        void update_success() {
            // given
            ActivityRequest request = createActivityRequest(6.0, 2000, 333, 350,
                    LocalDateTime.of(2025, 2, 1, 7, 0), "수정됨");

            given(activityRepository.findById(1L)).willReturn(Optional.of(testActivity));
            given(userRepository.save(any(User.class))).willReturn(testUser);

            // when
            ActivityResponse response = activityService.update(1L, 1L, request);

            // then
            assertThat(response).isNotNull();
            verify(activityRepository).findById(1L);
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 활동")
        void update_notFound_throwsException() {
            // given
            ActivityRequest request = createActivityRequest(6.0, 2000, null, null,
                    LocalDateTime.now(), null);

            given(activityRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> activityService.update(1L, 999L, request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("활동을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("성공 - 활동 삭제")
        void delete_success() {
            // given
            given(activityRepository.findById(1L)).willReturn(Optional.of(testActivity));
            given(userRepository.save(any(User.class))).willReturn(testUser);
            doNothing().when(activityRepository).delete(testActivity);

            // when
            activityService.delete(1L, 1L);

            // then
            verify(activityRepository).findById(1L);
            verify(activityRepository).delete(testActivity);
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 활동")
        void delete_notFound_throwsException() {
            // given
            given(activityRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> activityService.delete(1L, 999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("활동을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("getStats()")
    class GetStats {

        @Test
        @DisplayName("성공 - 전체 통계 조회")
        void getStats_all_success() {
            // given
            Page<RunningActivity> page = new PageImpl<>(List.of(testActivity));
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(activityRepository.findByUserIdOrderByStartedAtDesc(eq(1L), any(Pageable.class)))
                    .willReturn(page);

            // when
            ActivityStatsResponse response = activityService.getStats(1L, null, null);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getTotalCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("성공 - 연도/월 지정 통계 조회")
        void getStats_withYearMonth_success() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(activityRepository.sumDistanceByUserIdAndDateRange(eq(1L), any(), any()))
                    .willReturn(5.0);
            Page<RunningActivity> page = new PageImpl<>(List.of(testActivity));
            given(activityRepository.findByUserIdOrderByStartedAtDesc(eq(1L), any(Pageable.class)))
                    .willReturn(page);

            // when
            ActivityStatsResponse response = activityService.getStats(1L, 2025, 2);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getTotalDistance()).isEqualTo(5.0);
        }
    }

    @Nested
    @DisplayName("getSummary()")
    class GetSummary {

        @Test
        @DisplayName("성공 - 주간/월간 요약 조회")
        void getSummary_success() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(activityRepository.sumDistanceByUserIdAndDateRange(eq(1L), any(), any()))
                    .willReturn(10.0);
            given(activityRepository.countByUserIdAndDateRange(eq(1L), any(), any()))
                    .willReturn(2L);
            given(activityRepository.findByUserIdAndStartedAtBetween(eq(1L), any(), any()))
                    .willReturn(List.of(testActivity));

            // when
            ActivitySummaryResponse response = activityService.getSummary(1L);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getThisWeek()).isNotNull();
            assertThat(response.getThisMonth()).isNotNull();
            assertThat(response.getLastMonth()).isNotNull();
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사용자")
        void getSummary_userNotFound_throwsException() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> activityService.getSummary(999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("사용자를 찾을 수 없습니다");
        }
    }
}
