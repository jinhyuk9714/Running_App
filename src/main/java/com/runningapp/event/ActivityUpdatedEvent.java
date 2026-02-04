package com.runningapp.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * 러닝 활동 수정 이벤트
 *
 * 거리 변경 시 발행되어 레벨 재계산 트리거
 */
@Getter
public class ActivityUpdatedEvent extends ApplicationEvent {

    private final Long userId;
    private final Long activityId;
    private final double oldDistance;
    private final double newDistance;
    private final LocalDateTime startedAt;

    public ActivityUpdatedEvent(Object source, Long userId, Long activityId, double oldDistance, double newDistance, LocalDateTime startedAt) {
        super(source);
        this.userId = userId;
        this.activityId = activityId;
        this.oldDistance = oldDistance;
        this.newDistance = newDistance;
        this.startedAt = startedAt;
    }
}
