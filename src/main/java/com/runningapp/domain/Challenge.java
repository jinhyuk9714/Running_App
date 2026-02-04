package com.runningapp.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 챌린지 엔티티
 *
 * 진행중인 챌린지 목록: start_date <= today <= end_date
 */
@Entity
@Table(name = "challenges", indexes = {
    // 활성 챌린지 조회 (start_date <= today <= end_date)
    @Index(name = "idx_challenges_dates", columnList = "start_date, end_date"),
    // 만료 챌린지 배치 처리
    @Index(name = "idx_challenges_end_date", columnList = "end_date")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Challenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "target_distance")
    private Double targetDistance;  // km (DISTANCE 타입일 때)

    @Column(name = "target_count")
    private Integer targetCount;  // 횟수 (COUNT 타입일 때)

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChallengeType type;

    @Column(name = "recommended_min_level")
    private Integer recommendedMinLevel;  // 추천 최소 레벨 (1~10, null이면 모든 레벨)

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /** 진행 중인 챌린지인지 (오늘 기준) */
    public boolean isActive(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }
}
