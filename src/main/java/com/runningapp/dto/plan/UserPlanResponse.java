package com.runningapp.dto.plan;

import com.runningapp.domain.UserPlan;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "내 플랜 진행 정보 응답")
@Getter
@Builder
public class UserPlanResponse {

    @Schema(description = "참여 ID")
    private Long id;
    @Schema(description = "플랜 정보")
    private PlanResponse plan;
    @Schema(description = "시작일")
    private LocalDateTime startedAt;
    @Schema(description = "현재 주차")
    private Integer currentWeek;
    @Schema(description = "진행중 여부")
    private Boolean inProgress;
    @Schema(description = "완료일")
    private LocalDateTime completedAt;

    public static UserPlanResponse from(UserPlan userPlan) {
        return UserPlanResponse.builder()
                .id(userPlan.getId())
                .plan(PlanResponse.from(userPlan.getPlan()))
                .startedAt(userPlan.getStartedAt())
                .currentWeek(userPlan.getCurrentWeek())
                .inProgress(userPlan.isInProgress())
                .completedAt(userPlan.getCompletedAt())
                .build();
    }
}
