package com.runningapp.event.listener;

import com.runningapp.event.ActivityCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 트레이닝 플랜 이벤트 리스너
 *
 * 활동 생성 시 비동기로 진행 중인 플랜 주차 진행 체크
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrainingPlanEventListener {

    private final AsyncEventProcessor asyncEventProcessor;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleActivityCompleted(ActivityCompletedEvent event) {
        asyncEventProcessor.processPlanProgress(
                event.getUserId(),
                event.getDistance(),
                event.getStartedAt()
        );
    }
}
