package com.runningapp.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 러닝 활동 엔티티
 *
 * @ManyToOne(fetch = LAZY): N:1 관계, 지연 로딩으로 user 조회 시 추가 쿼리
 * @JdbcTypeCode(JSON): Hibernate 6+ 에서 JSON 타입 매핑 (GPS 경로 저장)
 */
@Entity
@Table(name = "running_activities", indexes = {
    // 사용자별 활동 목록 조회 (페이징) - 가장 자주 사용
    @Index(name = "idx_running_activities_user_started", columnList = "user_id, started_at DESC"),
    // 기간별 통계 조회
    @Index(name = "idx_running_activities_started", columnList = "started_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RunningActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)  // N:1, 지연 로딩 (필요할 때만 user 조회)
    @JoinColumn(name = "user_id", nullable = false)  // FK 컬럼명
    private User user;

    @Column(nullable = false)
    private Double distance;  // 거리 (km)

    @Column(nullable = false)
    private Integer duration;  // 러닝 시간 (초)

    @Column(name = "average_pace")
    private Integer averagePace;  // 평균 페이스: 초/km (예: 360 = 6분/km)

    private Integer calories;  // 예상 칼로리 소모

    @Column(name = "average_heart_rate")
    private Integer averageHeartRate;  // 평균 심박수 (bpm)

    @Column(name = "cadence")
    private Integer cadence;  // 평균 케이던스 (steps/min, SPM)

    @JdbcTypeCode(SqlTypes.JSON)  // JSON 직렬화 저장 (H2: CLOB, PostgreSQL: jsonb)
    @Column(columnDefinition = "text")  // H2/PostgreSQL 공통 (PostgreSQL은 text로 JSON 저장)
    private List<Map<String, Object>> route;  // GPS 경로: [{lat, lng, timestamp}, ...]

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /** 엔티티 수정 (불변 객체 대신 업데이트 메서드로 변경 사항 반영) */
    public void update(Double distance, Integer duration, Integer averagePace, Integer calories,
                       Integer averageHeartRate, Integer cadence, List<Map<String, Object>> route,
                       LocalDateTime startedAt, String memo) {
        this.distance = distance;
        this.duration = duration;
        this.averagePace = averagePace;
        this.calories = calories;
        this.averageHeartRate = averageHeartRate;
        this.cadence = cadence;
        this.route = route;
        this.startedAt = startedAt;
        this.memo = memo;
    }
}
