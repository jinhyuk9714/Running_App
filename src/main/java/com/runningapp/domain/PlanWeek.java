package com.runningapp.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * 플랜별 주차별 스케줄
 */
@Entity
@Table(name = "plan_weeks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PlanWeek {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private TrainingPlan plan;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column(name = "target_distance")
    private Double targetDistance;  // km

    @Column(name = "target_runs")
    private Integer targetRuns;  // 해당 주 목표 러닝 횟수

    @Column(columnDefinition = "TEXT")
    private String description;
}
