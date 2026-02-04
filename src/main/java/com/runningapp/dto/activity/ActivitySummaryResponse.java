package com.runningapp.dto.activity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 주간/월간 요약 통계 응답 (GET /api/activities/summary) */
@Schema(description = "주간·월간 요약 통계 응답")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivitySummaryResponse {

    @Schema(description = "이번 주 (월~일) 요약")
    private PeriodSummary thisWeek;
    @Schema(description = "이번 달 요약")
    private PeriodSummary thisMonth;
    @Schema(description = "지난달 요약")
    private PeriodSummary lastMonth;
}
