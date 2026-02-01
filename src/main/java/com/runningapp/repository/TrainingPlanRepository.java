package com.runningapp.repository;

import com.runningapp.domain.GoalType;
import com.runningapp.domain.PlanDifficulty;
import com.runningapp.domain.TrainingPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * TrainingPlan 레포지토리
 */
public interface TrainingPlanRepository extends JpaRepository<TrainingPlan, Long> {

    List<TrainingPlan> findByGoalTypeOrderByDifficulty(GoalType goalType);

    List<TrainingPlan> findByGoalTypeAndDifficulty(GoalType goalType, PlanDifficulty difficulty);
}
