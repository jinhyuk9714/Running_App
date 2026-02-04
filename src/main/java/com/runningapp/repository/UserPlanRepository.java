package com.runningapp.repository;

import com.runningapp.domain.UserPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * UserPlan 레포지토리
 */
public interface UserPlanRepository extends JpaRepository<UserPlan, Long> {

    List<UserPlan> findByUserIdOrderByStartedAtDesc(Long userId);

    Optional<UserPlan> findByUserIdAndPlanId(Long userId, Long planId);

    boolean existsByUserIdAndPlanIdAndCompletedAtIsNull(Long userId, Long planId);

    /** 내 플랜 조회 - TrainingPlan JOIN FETCH로 N+1 해결 */
    @Query("SELECT up FROM UserPlan up " +
           "JOIN FETCH up.plan " +
           "WHERE up.user.id = :userId " +
           "ORDER BY up.startedAt DESC")
    List<UserPlan> findByUserIdWithPlan(@Param("userId") Long userId);

    /** 진행중인 플랜 - TrainingPlan JOIN FETCH로 N+1 해결 */
    @Query("SELECT up FROM UserPlan up " +
           "JOIN FETCH up.plan " +
           "WHERE up.user.id = :userId AND up.completedAt IS NULL")
    List<UserPlan> findActiveByUserId(@Param("userId") Long userId);
}
