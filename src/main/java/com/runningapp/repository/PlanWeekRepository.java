package com.runningapp.repository;

import com.runningapp.domain.PlanWeek;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * PlanWeek 레포지토리
 */
public interface PlanWeekRepository extends JpaRepository<PlanWeek, Long> {

    List<PlanWeek> findByPlanIdOrderByWeekNumberAsc(Long planId);
}
