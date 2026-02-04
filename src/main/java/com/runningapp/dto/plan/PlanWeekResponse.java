package com.runningapp.dto.plan;

import com.runningapp.domain.PlanWeek;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "플랜 주차별 스케줄")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanWeekResponse {

    @Schema(description = "주차 번호")
    private Integer weekNumber;
    @Schema(description = "목표 거리 (km)")
    private Double targetDistance;
    @Schema(description = "목표 러닝 횟수")
    private Integer targetRuns;
    @Schema(description = "설명")
    private String description;

    public static PlanWeekResponse from(PlanWeek week) {
        return PlanWeekResponse.builder()
                .weekNumber(week.getWeekNumber())
                .targetDistance(week.getTargetDistance())
                .targetRuns(week.getTargetRuns())
                .description(week.getDescription())
                .build();
    }
}
