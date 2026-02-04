package com.runningapp.repository;

import com.runningapp.domain.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * Challenge 레포지토리
 */
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    /** 진행중인 챌린지 (start_date <= today <= end_date) */
    @Query("SELECT c FROM Challenge c WHERE c.startDate <= :date AND c.endDate >= :date ORDER BY c.startDate DESC")
    List<Challenge> findActiveByDate(@Param("date") LocalDate date);

    /** 종료일이 특정 날짜 이전인 챌린지 (만료 처리용) */
    List<Challenge> findByEndDateBefore(LocalDate date);
}
