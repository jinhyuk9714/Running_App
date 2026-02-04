package com.runningapp.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * 러닝 활동 생성 이벤트
 *
 * 활동 저장 후 발행되어 비동기로 레벨/챌린지/플랜 업데이트 트리거
 */
@Getter
public class ActivityCompletedEvent extends ApplicationEvent {

    private final Long userId;
    private final Long activityId;
    private final double distance;
    private final LocalDateTime startedAt;

    public ActivityCompletedEvent(Object source, Long userId, Long activityId, double distance, LocalDateTime startedAt) {
        super(source);
        this.userId = userId;
        this.activityId = activityId;
        this.distance = distance;
        this.startedAt = startedAt;
    }
}
