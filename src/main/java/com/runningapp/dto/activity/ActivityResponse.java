package com.runningapp.dto.activity;

import com.runningapp.domain.RunningActivity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 러닝 활동 응답 DTO
 *
 * from(): Entity → DTO 변환 (정적 팩토리 메서드)
 * Entity를 직접 반환하지 않고 DTO로 감싸서 응답 (순환참조, 불필요 필드 노출 방지)
 */
@Schema(description = "러닝 활동 응답")
@Getter
@Builder
public class ActivityResponse {

    @Schema(description = "활동 ID")
    private Long id;
    @Schema(description = "거리 (km)")
    private Double distance;
    @Schema(description = "러닝 시간 (초)")
    private Integer duration;
    @Schema(description = "평균 페이스 (초/km)")
    private Integer averagePace;
    @Schema(description = "칼로리")
    private Integer calories;
    @Schema(description = "평균 심박수 (bpm)")
    private Integer averageHeartRate;
    @Schema(description = "평균 케이던스 (SPM)")
    private Integer cadence;
    @Schema(description = "GPS 경로")
    private List<Map<String, Object>> route;
    @Schema(description = "시작 시간")
    private LocalDateTime startedAt;
    @Schema(description = "메모")
    private String memo;
    @Schema(description = "생성 시간")
    private LocalDateTime createdAt;

    /** Entity를 Response DTO로 변환 */
    public static ActivityResponse from(RunningActivity activity) {
        return ActivityResponse.builder()
                .id(activity.getId())
                .distance(activity.getDistance())
                .duration(activity.getDuration())
                .averagePace(activity.getAveragePace())
                .calories(activity.getCalories())
                .averageHeartRate(activity.getAverageHeartRate())
                .cadence(activity.getCadence())
                .route(activity.getRoute())
                .startedAt(activity.getStartedAt())
                .memo(activity.getMemo())
                .createdAt(activity.getCreatedAt())
                .build();
    }
}
