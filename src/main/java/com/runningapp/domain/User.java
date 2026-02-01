package com.runningapp.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 사용자 엔티티 (JPA Entity)
 *
 * JPA: Java Persistence API - 객체와 DB 테이블을 매핑하는 ORM 기술
 * @Entity: 이 클래스가 DB 테이블과 매핑됨
 * @Table: 실제 DB 테이블명 지정 (users)
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA는 기본 생성자 필요
@AllArgsConstructor
@Builder
public class User {

    @Id  // Primary Key
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // DB가 자동 증가 (AUTO_INCREMENT)
    private Long id;

    @Column(nullable = false, unique = true)  // NOT NULL, UNIQUE 제약조건
    private String email;

    @Column(nullable = false)  // 암호화된 비밀번호 저장 (BCrypt)
    private String password;

    @Column(nullable = false)
    private String nickname;

    private Double weight;   // 선택 입력
    private Double height;   // 선택 입력

    @Builder.Default  // Builder 사용 시 기본값
    private Integer level = 1;

    @Builder.Default
    private Double totalDistance = 0.0;  // 누적 러닝 거리 (km)

    @Column(name = "created_at", nullable = false, updatable = false)  // snake_case로 DB 컬럼명
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist  // INSERT 전에 자동 실행
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate  // UPDATE 전에 자동 실행
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /** 누적 거리 업데이트 (활동 생성/수정/삭제 시 사용) */
    public void addDistance(double distance) {
        this.totalDistance += distance;
    }

    /** 레벨 업데이트 (totalDistance 기반) */
    public void updateLevel() {
        this.level = com.runningapp.util.LevelCalculator.calculateLevel(this.totalDistance);
    }

    /** 프로필 수정 (null이 아닌 필드만 반영) */
    public void updateProfile(String nickname, Double weight, Double height) {
        if (nickname != null) this.nickname = nickname;
        if (weight != null) this.weight = weight;
        if (height != null) this.height = height;
    }
}
