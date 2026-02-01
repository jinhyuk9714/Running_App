package com.runningapp.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 사용자-챌린지 참여 엔티티
 *
 * 사용자가 챌린지에 참여하고 진행률을 추적
 */
@Entity
@Table(name = "user_challenges")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @Column(name = "current_distance")
    @Builder.Default
    private Double currentDistance = 0.0;

    @Column(name = "current_count")
    @Builder.Default
    private Integer currentCount = 0;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;  // null이면 미완료

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
    }

    /** 진행률 업데이트 (거리) */
    public void addDistance(double distance) {
        this.currentDistance += distance;
    }

    /** 진행률 업데이트 (횟수) */
    public void addCount() {
        this.currentCount++;
    }

    /** 완료 처리 */
    public void complete() {
        this.completedAt = LocalDateTime.now();
    }

    /** 완료 여부 */
    public boolean isCompleted() {
        return completedAt != null;
    }

    /** 목표 달성 여부 (챌린지 타입에 따라) */
    public boolean isGoalAchieved() {
        if (challenge.getType() == ChallengeType.DISTANCE) {
            Double target = challenge.getTargetDistance();
            return target != null && currentDistance >= target;
        } else {
            Integer target = challenge.getTargetCount();
            return target != null && currentCount >= target;
        }
    }
}
