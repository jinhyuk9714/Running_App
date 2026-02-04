package com.runningapp.repository;

import com.runningapp.domain.PlanWeek;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * PlanWeek 레포지토리
 */
public interface PlanWeekRepository extends JpaRepository<PlanWeek, Long> {

    List<PlanWeek> findByPlanIdOrderByWeekNumberAsc(Long planId);

    /** 여러 플랜의 주차 정보를 한 번에 조회 (N+1 해결) */
    @Query("SELECT pw FROM PlanWeek pw WHERE pw.plan.id IN :planIds ORDER BY pw.plan.id, pw.weekNumber")
    List<PlanWeek> findByPlanIds(@Param("planIds") List<Long> planIds);
}
