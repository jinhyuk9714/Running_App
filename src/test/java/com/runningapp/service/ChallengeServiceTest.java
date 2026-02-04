package com.runningapp.service;

import com.runningapp.domain.Challenge;
import com.runningapp.domain.ChallengeType;
import com.runningapp.domain.User;
import com.runningapp.domain.UserChallenge;
import com.runningapp.dto.challenge.ChallengeResponse;
import com.runningapp.dto.challenge.UserChallengeResponse;
import com.runningapp.exception.BadRequestException;
import com.runningapp.exception.NotFoundException;
import com.runningapp.repository.ChallengeRepository;
import com.runningapp.repository.UserChallengeRepository;
import com.runningapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChallengeService 단위 테스트")
class ChallengeServiceTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private UserChallengeRepository userChallengeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChallengeService challengeService;

    private User testUser;
    private Challenge testChallenge;
    private UserChallenge testUserChallenge;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@test.com")
                .password("encodedPassword")
                .nickname("테스터")
                .build();
        setField(testUser, "id", 1L);
        setField(testUser, "level", 3);

        testChallenge = Challenge.builder()
                .name("10km 챌린지")
                .description("한 달간 10km 달리기")
                .type(ChallengeType.DISTANCE)
                .targetDistance(10.0)
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().plusDays(25))
                .recommendedMinLevel(1)
                .build();
        setField(testChallenge, "id", 1L);

        testUserChallenge = UserChallenge.builder()
                .user(testUser)
                .challenge(testChallenge)
                .build();
        setField(testUserChallenge, "id", 1L);
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
    @DisplayName("getActiveChallenges()")
    class GetActiveChallenges {

        @Test
        @DisplayName("성공 - 활성 챌린지 목록 조회")
        void getActiveChallenges_success() {
            // given
            given(challengeRepository.findActiveByDate(any(LocalDate.class)))
                    .willReturn(List.of(testChallenge));

            // when
            List<ChallengeResponse> result = challengeService.getActiveChallenges();

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("10km 챌린지");
        }
    }

    @Nested
    @DisplayName("getRecommendedChallenges()")
    class GetRecommendedChallenges {

        @Test
        @DisplayName("성공 - 추천 챌린지 목록 조회")
        void getRecommendedChallenges_success() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(challengeRepository.findActiveByDate(any(LocalDate.class)))
                    .willReturn(List.of(testChallenge));
            given(userChallengeRepository.existsByUserIdAndChallengeId(1L, 1L))
                    .willReturn(false);

            // when
            List<ChallengeResponse> result = challengeService.getRecommendedChallenges(1L);

            // then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("이미 참여한 챌린지는 제외")
        void getRecommendedChallenges_excludesJoined() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(challengeRepository.findActiveByDate(any(LocalDate.class)))
                    .willReturn(List.of(testChallenge));
            given(userChallengeRepository.existsByUserIdAndChallengeId(1L, 1L))
                    .willReturn(true);

            // when
            List<ChallengeResponse> result = challengeService.getRecommendedChallenges(1L);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사용자")
        void getRecommendedChallenges_userNotFound_throwsException() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> challengeService.getRecommendedChallenges(999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("사용자를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("joinChallenge()")
    class JoinChallenge {

        @Test
        @DisplayName("성공 - 챌린지 참여")
        void joinChallenge_success() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(challengeRepository.findById(1L)).willReturn(Optional.of(testChallenge));
            given(userChallengeRepository.existsByUserIdAndChallengeId(1L, 1L)).willReturn(false);
            given(userChallengeRepository.save(any(UserChallenge.class))).willAnswer(invocation -> {
                UserChallenge uc = invocation.getArgument(0);
                setField(uc, "id", 1L);
                return uc;
            });

            // when
            UserChallengeResponse response = challengeService.joinChallenge(1L, 1L);

            // then
            assertThat(response).isNotNull();
            verify(userChallengeRepository).save(any(UserChallenge.class));
        }

        @Test
        @DisplayName("실패 - 이미 참여 중인 챌린지")
        void joinChallenge_alreadyJoined_throwsException() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(challengeRepository.findById(1L)).willReturn(Optional.of(testChallenge));
            given(userChallengeRepository.existsByUserIdAndChallengeId(1L, 1L)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> challengeService.joinChallenge(1L, 1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("이미 참여 중인 챌린지입니다");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 챌린지")
        void joinChallenge_challengeNotFound_throwsException() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(challengeRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> challengeService.joinChallenge(1L, 999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("챌린지를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("실패 - 참여 기간이 아닌 챌린지")
        void joinChallenge_notActive_throwsException() {
            // given
            Challenge expiredChallenge = Challenge.builder()
                    .name("만료된 챌린지")
                    .type(ChallengeType.DISTANCE)
                    .targetDistance(10.0)
                    .startDate(LocalDate.now().minusDays(30))
                    .endDate(LocalDate.now().minusDays(1))
                    .build();
            setField(expiredChallenge, "id", 2L);

            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(challengeRepository.findById(2L)).willReturn(Optional.of(expiredChallenge));
            given(userChallengeRepository.existsByUserIdAndChallengeId(1L, 2L)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> challengeService.joinChallenge(1L, 2L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("참여 기간이 아닌 챌린지입니다");
        }
    }

    @Nested
    @DisplayName("getMyChallenges()")
    class GetMyChallenges {

        @Test
        @DisplayName("성공 - 내 챌린지 목록 조회")
        void getMyChallenges_success() {
            // given
            given(userChallengeRepository.findByUserIdOrderByJoinedAtDesc(1L))
                    .willReturn(List.of(testUserChallenge));

            // when
            List<UserChallengeResponse> result = challengeService.getMyChallenges(1L);

            // then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getChallengeProgress()")
    class GetChallengeProgress {

        @Test
        @DisplayName("성공 - 챌린지 진행률 조회")
        void getChallengeProgress_success() {
            // given
            given(userChallengeRepository.findByUserIdAndChallengeId(1L, 1L))
                    .willReturn(Optional.of(testUserChallenge));

            // when
            UserChallengeResponse response = challengeService.getChallengeProgress(1L, 1L);

            // then
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("실패 - 참여하지 않은 챌린지")
        void getChallengeProgress_notJoined_throwsException() {
            // given
            given(userChallengeRepository.findByUserIdAndChallengeId(1L, 999L))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> challengeService.getChallengeProgress(1L, 999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("참여한 챌린지를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("updateProgressOnActivity()")
    class UpdateProgressOnActivity {

        @Test
        @DisplayName("성공 - 거리 챌린지 진행률 업데이트")
        void updateProgressOnActivity_distance_success() {
            // given
            given(userChallengeRepository.findActiveByUserId(1L))
                    .willReturn(List.of(testUserChallenge));

            // when
            challengeService.updateProgressOnActivity(1L, 5.0, LocalDate.now());

            // then
            verify(userChallengeRepository).findActiveByUserId(1L);
        }
    }
}
