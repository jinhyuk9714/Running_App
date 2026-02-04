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
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 챌린지 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final UserChallengeRepository userChallengeRepository;
    private final UserRepository userRepository;

    /** 진행중인 챌린지 목록 */
    @Cacheable(value = "activeChallenges", key = "'all'")
    public List<ChallengeResponse> getActiveChallenges() {
        return challengeRepository.findActiveByDate(LocalDate.now()).stream()
                .map(ChallengeResponse::from)
                .toList();
    }

    /** 추천 챌린지 (레벨/목표 기반 - 진행중인 챌린지 중 미참여, 레벨에 맞는 것 우선) */
    @Cacheable(value = "recommendedChallenges", key = "#userId")
    public List<ChallengeResponse> getRecommendedChallenges(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

        // N+1 해결: 참여한 챌린지 ID를 한 번에 조회
        List<Long> joinedChallengeIds = userChallengeRepository.findChallengeIdsByUserId(userId);

        List<Challenge> active = challengeRepository.findActiveByDate(LocalDate.now());
        return active.stream()
                .filter(c -> !joinedChallengeIds.contains(c.getId()))
                .filter(c -> c.getRecommendedMinLevel() == null || c.getRecommendedMinLevel() <= user.getLevel())
                .sorted((a, b) -> {
                    // 레벨에 가까운 챌린지 우선 (recommendedMinLevel이 user.level에 가까운 순)
                    int levelDiffA = Math.abs((a.getRecommendedMinLevel() != null ? a.getRecommendedMinLevel() : 1) - user.getLevel());
                    int levelDiffB = Math.abs((b.getRecommendedMinLevel() != null ? b.getRecommendedMinLevel() : 1) - user.getLevel());
                    return Integer.compare(levelDiffA, levelDiffB);
                })
                .map(ChallengeResponse::from)
                .toList();
    }

    @Transactional
    @CacheEvict(value = "recommendedChallenges", key = "#userId")
    public UserChallengeResponse joinChallenge(Long userId, Long challengeId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new NotFoundException("챌린지를 찾을 수 없습니다"));

        if (userChallengeRepository.existsByUserIdAndChallengeId(userId, challengeId)) {
            throw new BadRequestException("이미 참여 중인 챌린지입니다");
        }

        if (!challenge.isActive(LocalDate.now())) {
            throw new BadRequestException("참여 기간이 아닌 챌린지입니다");
        }

        UserChallenge userChallenge = UserChallenge.builder()
                .user(user)
                .challenge(challenge)
                .build();
        userChallenge = userChallengeRepository.save(userChallenge);

        return UserChallengeResponse.from(userChallenge);
    }

    /** 내 참여 챌린지 목록 - JOIN FETCH로 N+1 해결 */
    public List<UserChallengeResponse> getMyChallenges(Long userId) {
        return userChallengeRepository.findByUserIdWithChallenge(userId).stream()
                .map(UserChallengeResponse::from)
                .toList();
    }

    /** 챌린지 진행률 조회 - JOIN FETCH로 N+1 해결 */
    public UserChallengeResponse getChallengeProgress(Long userId, Long challengeId) {
        UserChallenge userChallenge = userChallengeRepository.findByUserIdAndChallengeIdWithChallenge(userId, challengeId)
                .orElseThrow(() -> new NotFoundException("참여한 챌린지를 찾을 수 없습니다"));
        return UserChallengeResponse.from(userChallenge);
    }

    /** 러닝 활동 저장 시 호출 - 참여중인 챌린지 진행률 업데이트 */
    @Transactional
    public void updateProgressOnActivity(Long userId, double distance, LocalDate activityDate) {
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
            }
        }
    }
}
