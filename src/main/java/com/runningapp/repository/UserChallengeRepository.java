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

    /** 진행중인 참여 (미완료) */
    @Query("SELECT uc FROM UserChallenge uc " +
           "JOIN uc.challenge c " +
           "WHERE uc.user.id = :userId AND uc.completedAt IS NULL " +
           "AND c.startDate <= CURRENT_DATE AND c.endDate >= CURRENT_DATE")
    List<UserChallenge> findActiveByUserId(@Param("userId") Long userId);

    /** 특정 챌린지의 미완료 참여자 (만료 처리용) */
    List<UserChallenge> findByChallengeIdAndCompletedAtIsNull(Long challengeId);
}
