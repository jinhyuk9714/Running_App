package com.runningapp.dto.plan;

import com.runningapp.domain.GoalType;
import com.runningapp.domain.PlanDifficulty;
import com.runningapp.domain.TrainingPlan;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "트레이닝 플랜 응답")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanResponse {

    @Schema(description = "플랜 ID")
    private Long id;
    @Schema(description = "플랜명")
    private String name;
    @Schema(description = "설명")
    private String description;
    @Schema(description = "목표 유형")
    private GoalType goalType;
    @Schema(description = "난이도")
    private PlanDifficulty difficulty;
    @Schema(description = "총 주차")
    private Integer totalWeeks;
    @Schema(description = "총 러닝 횟수")
    private Integer totalRuns;
    @Schema(description = "생성일")
    private LocalDateTime createdAt;

    public static PlanResponse from(TrainingPlan plan) {
        return PlanResponse.builder()
                .id(plan.getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .goalType(plan.getGoalType())
                .difficulty(plan.getDifficulty())
                .totalWeeks(plan.getTotalWeeks())
                .totalRuns(plan.getTotalRuns())
                .createdAt(plan.getCreatedAt())
                .build();
    }
}
