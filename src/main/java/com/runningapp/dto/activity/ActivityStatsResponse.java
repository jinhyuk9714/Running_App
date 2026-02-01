package com.runningapp.dto.activity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/** 러닝 통계 응답 DTO (GET /api/activities/stats) */
@Schema(description = "러닝 통계 응답")
@Getter
@Builder
public class ActivityStatsResponse {

    @Schema(description = "총 거리 (km)")
    private Double totalDistance;
    @Schema(description = "활동 횟수")
    private Integer totalCount;
    @Schema(description = "총 러닝 시간 (초)")
    private Integer totalDuration;
    @Schema(description = "평균 페이스 (초/km)")
    private Integer averagePace;
}
