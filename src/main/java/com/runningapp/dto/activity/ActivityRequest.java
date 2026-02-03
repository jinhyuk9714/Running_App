package com.runningapp.dto.activity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 러닝 활동 저장/수정 요청 DTO
 *
 * @Positive: 0 초과만 허용
 * @PositiveOrZero: 0 이상 허용 (선택 필드)
 */
@Schema(description = "러닝 활동 저장/수정 요청")
@Getter
@NoArgsConstructor
public class ActivityRequest {

    @Schema(description = "러닝 거리 (km)", example = "5.2", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "거리는 필수입니다")
    @Positive(message = "거리는 0보다 커야 합니다")
    private Double distance;

    @Schema(description = "러닝 시간 (초)", example = "1800", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "시간은 필수입니다")
    @Positive(message = "시간은 0보다 커야 합니다")
    private Integer duration;

    @Schema(description = "평균 페이스 (초/km, 예: 360=6분/km)", example = "346")
    @PositiveOrZero(message = "평균 페이스는 0 이상이어야 합니다")
    private Integer averagePace;

    @Schema(description = "예상 칼로리 소모", example = "300")
    @PositiveOrZero(message = "칼로리는 0 이상이어야 합니다")
    private Integer calories;

    @Schema(description = "평균 심박수 (bpm)", example = "145")
    @PositiveOrZero(message = "심박수는 0 이상이어야 합니다")
    private Integer averageHeartRate;

    @Schema(description = "평균 케이던스 (steps/min, SPM)", example = "170")
    @PositiveOrZero(message = "케이던스는 0 이상이어야 합니다")
    private Integer cadence;

    @Schema(description = "GPS 경로 [{lat, lng, timestamp}, ...]")
    private List<Map<String, Object>> route;

    @Schema(description = "러닝 시작 시간", example = "2025-02-01T07:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "시작 시간은 필수입니다")
    private LocalDateTime startedAt;

    @Schema(description = "메모")
    private String memo;
}
