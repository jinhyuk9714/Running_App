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

    /** 진행중인 플랜 */
    @Query("SELECT up FROM UserPlan up WHERE up.user.id = :userId AND up.completedAt IS NULL")
    List<UserPlan> findActiveByUserId(@Param("userId") Long userId);
}
