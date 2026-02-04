package com.runningapp.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * 러닝 활동 삭제 이벤트
 *
 * 삭제 전 발행되어 레벨 재계산 트리거
 */
@Getter
public class ActivityDeletedEvent extends ApplicationEvent {

    private final Long userId;
    private final Long activityId;
    private final double distance;
    private final LocalDateTime startedAt;

    public ActivityDeletedEvent(Object source, Long userId, Long activityId, double distance, LocalDateTime startedAt) {
        super(source);
        this.userId = userId;
        this.activityId = activityId;
        this.distance = distance;
        this.startedAt = startedAt;
    }
}
