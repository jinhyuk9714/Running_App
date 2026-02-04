package com.runningapp.event.listener;

import com.runningapp.event.ActivityCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 챌린지 진행률 이벤트 리스너
 *
 * 활동 생성 시 비동기로 참여 중인 챌린지 진행률 업데이트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChallengeProgressEventListener {

    private final AsyncEventProcessor asyncEventProcessor;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleActivityCompleted(ActivityCompletedEvent event) {
        asyncEventProcessor.processChallengeProgress(
                event.getUserId(),
                event.getDistance(),
                event.getStartedAt().toLocalDate()
        );
    }
}
