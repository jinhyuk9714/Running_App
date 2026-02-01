package com.runningapp.dto.challenge;

import com.runningapp.domain.ChallengeType;
import com.runningapp.domain.UserChallenge;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "내 챌린지 참여 정보 응답")
@Getter
@Builder
public class UserChallengeResponse {

    @Schema(description = "참여 ID")
    private Long id;
    @Schema(description = "챌린지 정보")
    private ChallengeResponse challenge;
    @Schema(description = "현재 진행 거리 (km)")
    private Double currentDistance;
    @Schema(description = "현재 진행 횟수")
    private Integer currentCount;
    @Schema(description = "목표 거리 (km)")
    private Double targetDistance;
    @Schema(description = "목표 횟수")
    private Integer targetCount;
    @Schema(description = "진행률 (%)")
    private Integer progressPercent;
    @Schema(description = "완료 여부")
    private Boolean completed;
    @Schema(description = "참여일")
    private LocalDateTime joinedAt;
    @Schema(description = "완료일")
    private LocalDateTime completedAt;

    public static UserChallengeResponse from(UserChallenge userChallenge) {
        double progress = 0;
        if (userChallenge.getChallenge().getType() == ChallengeType.DISTANCE) {
            Double target = userChallenge.getChallenge().getTargetDistance();
            progress = (target != null && target > 0)
                    ? Math.min(100, (userChallenge.getCurrentDistance() / target) * 100) : 0;
        } else {
            Integer target = userChallenge.getChallenge().getTargetCount();
            progress = (target != null && target > 0)
                    ? Math.min(100, (userChallenge.getCurrentCount().doubleValue() / target) * 100) : 0;
        }

        return UserChallengeResponse.builder()
                .id(userChallenge.getId())
                .challenge(ChallengeResponse.from(userChallenge.getChallenge()))
                .currentDistance(userChallenge.getCurrentDistance())
                .currentCount(userChallenge.getCurrentCount())
                .targetDistance(userChallenge.getChallenge().getTargetDistance())
                .targetCount(userChallenge.getChallenge().getTargetCount())
                .progressPercent((int) progress)
                .completed(userChallenge.isCompleted())
                .joinedAt(userChallenge.getJoinedAt())
                .completedAt(userChallenge.getCompletedAt())
                .build();
    }
}
