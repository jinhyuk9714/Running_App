package com.runningapp.repository;

import com.runningapp.domain.UserChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * UserChallenge 레포지토리
 */
public interface UserChallengeRepository extends JpaRepository<UserChallenge, Long> {

    List<UserChallenge> findByUserIdOrderByJoinedAtDesc(Long userId);

    Optional<UserChallenge> findByUserIdAndChallengeId(Long userId, Long challengeId);

    boolean existsByUserIdAndChallengeId(Long userId, Long challengeId);

    /** 내 챌린지 조회 - Challenge JOIN FETCH로 N+1 해결 */
    @Query("SELECT uc FROM UserChallenge uc " +
           "JOIN FETCH uc.challenge " +
           "WHERE uc.user.id = :userId " +
           "ORDER BY uc.joinedAt DESC")
    List<UserChallenge> findByUserIdWithChallenge(@Param("userId") Long userId);

    /** 특정 챌린지 진행률 조회 - Challenge JOIN FETCH로 N+1 해결 */
    @Query("SELECT uc FROM UserChallenge uc " +
           "JOIN FETCH uc.challenge " +
           "WHERE uc.user.id = :userId AND uc.challenge.id = :challengeId")
    Optional<UserChallenge> findByUserIdAndChallengeIdWithChallenge(
            @Param("userId") Long userId, @Param("challengeId") Long challengeId);

    /** 진행중인 참여 (미완료) - Challenge JOIN FETCH로 N+1 해결 */
    @Query("SELECT uc FROM UserChallenge uc " +
           "JOIN FETCH uc.challenge c " +
           "WHERE uc.user.id = :userId AND uc.completedAt IS NULL " +
           "AND c.startDate <= CURRENT_DATE AND c.endDate >= CURRENT_DATE")
    List<UserChallenge> findActiveByUserId(@Param("userId") Long userId);

    /** 사용자가 참여한 챌린지 ID 목록 조회 (추천 필터링용) */
    @Query("SELECT uc.challenge.id FROM UserChallenge uc WHERE uc.user.id = :userId")
    List<Long> findChallengeIdsByUserId(@Param("userId") Long userId);

    /** 특정 챌린지의 미완료 참여자 (만료 처리용) */
    List<UserChallenge> findByChallengeIdAndCompletedAtIsNull(Long challengeId);
}
