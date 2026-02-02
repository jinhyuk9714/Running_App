package com.runningapp.repository;

import com.runningapp.domain.RunningActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RunningActivity 레포지토리
 *
 * @Query: JPQL(Java Persistence Query Language) - 엔티티 기준 쿼리
 * - COALESCE: NULL일 때 0 반환
 * - :userId, :start, :end: 파라미터 바인딩
 */
public interface RunningActivityRepository extends JpaRepository<RunningActivity, Long> {

    // userId로 조회, startedAt 내림차순, 페이징 지원
    Page<RunningActivity> findByUserIdOrderByStartedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(a.distance), 0) FROM RunningActivity a WHERE a.user.id = :userId " +
           "AND a.startedAt >= :start AND a.startedAt < :end")
    Double sumDistanceByUserIdAndDateRange(@Param("userId") Long userId,
                                           @Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(a) FROM RunningActivity a WHERE a.user.id = :userId " +
           "AND a.startedAt >= :start AND a.startedAt < :end")
    long countByUserIdAndDateRange(@Param("userId") Long userId,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

    List<RunningActivity> findByUserIdAndStartedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);
}
