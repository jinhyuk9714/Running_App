package com.runningapp.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 사용자 플랜 진행 엔티티
 *
 * 사용자가 플랜을 시작하고 진행률 추적
 */
@Entity
@Table(name = "user_plans")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private TrainingPlan plan;

    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "current_week")
    @Builder.Default
    private Integer currentWeek = 1;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;  // null이면 진행중

    @PrePersist
    protected void onCreate() {
        startedAt = LocalDateTime.now();
    }

    /** 완료 처리 */
    public void complete() {
        this.completedAt = LocalDateTime.now();
    }

    /** 진행중 여부 */
    public boolean isInProgress() {
        return completedAt == null;
    }

    /** 주차 진행 */
    public void advanceWeek() {
        this.currentWeek++;
    }
}
