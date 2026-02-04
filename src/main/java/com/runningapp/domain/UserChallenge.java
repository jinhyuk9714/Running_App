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
@Table(name = "user_challenges", indexes = {
    // 사용자별 참여 목록 조회
    @Index(name = "idx_user_challenges_user_joined", columnList = "user_id, joined_at DESC"),
    // 사용자+챌린지 중복 체크 및 진행률 조회 (UNIQUE 제약)
    @Index(name = "idx_user_challenges_user_challenge", columnList = "user_id, challenge_id", unique = true),
    // 활성 챌린지 필터링 (completedAt IS NULL)
    @Index(name = "idx_user_challenges_user_completed", columnList = "user_id, completed_at"),
    // 챌린지 만료 처리
    @Index(name = "idx_user_challenges_challenge_completed", columnList = "challenge_id, completed_at")
})
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

    /** 만료 처리 (기간 종료 시 미완료 상태로 마감) */
    public void markExpired() {
        if (this.completedAt == null) {
            this.completedAt = LocalDateTime.now();
        }
    }
}
