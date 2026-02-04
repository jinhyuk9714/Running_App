package com.runningapp.dto.challenge;

import com.runningapp.domain.Challenge;
import com.runningapp.domain.ChallengeType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "챌린지 응답")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeResponse {

    @Schema(description = "챌린지 ID")
    private Long id;
    @Schema(description = "챌린지명")
    private String name;
    @Schema(description = "설명")
    private String description;
    @Schema(description = "목표 거리 (km)")
    private Double targetDistance;
    @Schema(description = "목표 횟수")
    private Integer targetCount;
    @Schema(description = "시작일")
    private LocalDate startDate;
    @Schema(description = "종료일")
    private LocalDate endDate;
    @Schema(description = "챌린지 유형")
    private ChallengeType type;
    @Schema(description = "생성일")
    private LocalDateTime createdAt;

    public static ChallengeResponse from(Challenge challenge) {
        return ChallengeResponse.builder()
                .id(challenge.getId())
                .name(challenge.getName())
                .description(challenge.getDescription())
                .targetDistance(challenge.getTargetDistance())
                .targetCount(challenge.getTargetCount())
                .startDate(challenge.getStartDate())
                .endDate(challenge.getEndDate())
                .type(challenge.getType())
                .createdAt(challenge.getCreatedAt())
                .build();
    }
}
