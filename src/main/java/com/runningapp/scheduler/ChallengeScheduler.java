package com.runningapp.scheduler;

import com.runningapp.domain.Challenge;
import com.runningapp.domain.UserChallenge;
import com.runningapp.repository.ChallengeRepository;
import com.runningapp.repository.UserChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 챌린지 스케줄러
 *
 * 매일 00:05에 만료된 챌린지 참여자들을 만료 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChallengeScheduler {

    private final ChallengeRepository challengeRepository;
    private final UserChallengeRepository userChallengeRepository;

    @Scheduled(cron = "0 5 0 * * *")  // 매일 00:05
    @Transactional
    public void expireChallenges() {
        log.info("챌린지 만료 처리 시작");

        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<Challenge> expiredChallenges = challengeRepository.findByEndDateBefore(yesterday);

        int expiredCount = 0;
        for (Challenge challenge : expiredChallenges) {
            List<UserChallenge> incompleteParticipants =
                    userChallengeRepository.findByChallengeIdAndCompletedAtIsNull(challenge.getId());

            for (UserChallenge uc : incompleteParticipants) {
                uc.markExpired();
                expiredCount++;
            }
        }

        log.info("챌린지 만료 처리 완료: expiredChallenges={}, expiredParticipants={}",
                expiredChallenges.size(), expiredCount);
    }
}
