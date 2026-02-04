package com.runningapp.event.listener;

import com.runningapp.event.ActivityCompletedEvent;
import com.runningapp.event.ActivityDeletedEvent;
import com.runningapp.event.ActivityUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 사용자 레벨 이벤트 리스너
 *
 * 활동 생성/수정/삭제 시 비동기로 누적 거리 및 레벨 업데이트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserLevelEventListener {

    private final AsyncEventProcessor asyncEventProcessor;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleActivityCompleted(ActivityCompletedEvent event) {
        asyncEventProcessor.processLevelUpdate(event.getUserId(), event.getDistance(), "생성");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleActivityUpdated(ActivityUpdatedEvent event) {
        double diff = event.getNewDistance() - event.getOldDistance();
        asyncEventProcessor.processLevelUpdate(event.getUserId(), diff, "수정");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleActivityDeleted(ActivityDeletedEvent event) {
        asyncEventProcessor.processLevelUpdate(event.getUserId(), -event.getDistance(), "삭제");
    }
}
