package com.runningapp.config;

import com.runningapp.domain.Challenge;
import com.runningapp.domain.ChallengeType;
import com.runningapp.repository.ChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 챌린지 시드 데이터 로더
 *
 * 애플리케이션 시작 시 기본 챌린지 생성
 * - 이번 달 100km: 거리 목표
 * - 이번 달 12회 달리기: 횟수 목표
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")  // 테스트 시에는 실행 안 함
public class ChallengeDataLoader implements CommandLineRunner {

    private final ChallengeRepository challengeRepository;

    @Override
    public void run(String... args) {
        if (challengeRepository.count() > 0) {
            return;  // 이미 데이터 있으면 스킵
        }

        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());

        Challenge distance50 = Challenge.builder()
                .name("이번 달 50km 달리기")
                .description("초급자용! 한 달 동안 50km를 달성해보세요.")
                .targetDistance(50.0)
                .targetCount(null)
                .startDate(startOfMonth)
                .endDate(endOfMonth)
                .type(ChallengeType.DISTANCE)
                .recommendedMinLevel(1)
                .build();

        Challenge distance100 = Challenge.builder()
                .name("이번 달 100km 달리기")
                .description("한 달 동안 100km를 달성해보세요!")
                .targetDistance(100.0)
                .targetCount(null)
                .startDate(startOfMonth)
                .endDate(endOfMonth)
                .type(ChallengeType.DISTANCE)
                .recommendedMinLevel(4)
                .build();

        Challenge distance150 = Challenge.builder()
                .name("이번 달 150km 달리기")
                .description("고급자용! 한 달 동안 150km를 달성해보세요.")
                .targetDistance(150.0)
                .targetCount(null)
                .startDate(startOfMonth)
                .endDate(endOfMonth)
                .type(ChallengeType.DISTANCE)
                .recommendedMinLevel(7)
                .build();

        Challenge count8 = Challenge.builder()
                .name("이번 달 8회 달리기")
                .description("초급자용! 한 달 동안 8번 러닝에 도전하세요.")
                .targetDistance(null)
                .targetCount(8)
                .startDate(startOfMonth)
                .endDate(endOfMonth)
                .type(ChallengeType.COUNT)
                .recommendedMinLevel(1)
                .build();

        Challenge count12 = Challenge.builder()
                .name("이번 달 12회 달리기")
                .description("한 달 동안 12번 러닝에 도전하세요!")
                .targetDistance(null)
                .targetCount(12)
                .startDate(startOfMonth)
                .endDate(endOfMonth)
                .type(ChallengeType.COUNT)
                .recommendedMinLevel(3)
                .build();

        Challenge count16 = Challenge.builder()
                .name("이번 달 16회 달리기")
                .description("고급자용! 한 달 동안 16번 러닝에 도전하세요.")
                .targetDistance(null)
                .targetCount(16)
                .startDate(startOfMonth)
                .endDate(endOfMonth)
                .type(ChallengeType.COUNT)
                .recommendedMinLevel(6)
                .build();

        challengeRepository.save(distance50);
        challengeRepository.save(distance100);
        challengeRepository.save(distance150);
        challengeRepository.save(count8);
        challengeRepository.save(count12);
        challengeRepository.save(count16);

        log.info("챌린지 시드 데이터 로드 완료: {}개", 6);
    }
}
