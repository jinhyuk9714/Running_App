package com.runningapp.util;

import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;

import java.util.Map;

/**
 * 구조화된 로깅 유틸리티
 *
 * JSON 로그에 커스텀 필드를 쉽게 추가할 수 있도록 지원
 *
 * 사용 예시:
 * <pre>
 * // 단일 필드
 * LogUtils.info(log, "로그인 성공", "email", "user@test.com");
 *
 * // 다중 필드
 * LogUtils.info(log, "활동 저장", Map.of(
 *     "activityId", 123,
 *     "distance", 5.5,
 *     "duration", 1800
 * ));
 * </pre>
 *
 * JSON 출력 예시:
 * <pre>
 * {
 *   "message": "로그인 성공",
 *   "email": "user@test.com",
 *   "requestId": "abc123",
 *   ...
 * }
 * </pre>
 */
@Slf4j
public final class LogUtils {

    private LogUtils() {
        // 유틸리티 클래스
    }

    // ========== INFO 레벨 ==========

    /**
     * INFO 로그 - 단일 필드
     */
    public static void info(Logger logger, String message, String key, Object value) {
        logger.info(message + " {}", StructuredArguments.kv(key, value));
    }

    /**
     * INFO 로그 - 다중 필드
     */
    public static void info(Logger logger, String message, Map<String, Object> fields) {
        Object[] args = fields.entrySet().stream()
                .map(e -> StructuredArguments.kv(e.getKey(), e.getValue()))
                .toArray();
        logger.info(message + " " + "{} ".repeat(args.length).trim(), args);
    }

    /**
     * INFO 로그 - 키-값 쌍
     */
    public static void info(Logger logger, String message, Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            logger.info(message);
            return;
        }

        Object[] args = new Object[keyValues.length / 2];
        for (int i = 0; i < keyValues.length; i += 2) {
            args[i / 2] = StructuredArguments.kv(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        logger.info(message + " " + "{} ".repeat(args.length).trim(), args);
    }

    // ========== WARN 레벨 ==========

    /**
     * WARN 로그 - 단일 필드
     */
    public static void warn(Logger logger, String message, String key, Object value) {
        logger.warn(message + " {}", StructuredArguments.kv(key, value));
    }

    /**
     * WARN 로그 - 다중 필드
     */
    public static void warn(Logger logger, String message, Map<String, Object> fields) {
        Object[] args = fields.entrySet().stream()
                .map(e -> StructuredArguments.kv(e.getKey(), e.getValue()))
                .toArray();
        logger.warn(message + " " + "{} ".repeat(args.length).trim(), args);
    }

    // ========== ERROR 레벨 ==========

    /**
     * ERROR 로그 - 예외 포함
     */
    public static void error(Logger logger, String message, Throwable t, String key, Object value) {
        logger.error(message + " {}", StructuredArguments.kv(key, value), t);
    }

    /**
     * ERROR 로그 - 예외 + 다중 필드
     */
    public static void error(Logger logger, String message, Throwable t, Map<String, Object> fields) {
        Object[] args = new Object[fields.size() + 1];
        int i = 0;
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            args[i++] = StructuredArguments.kv(entry.getKey(), entry.getValue());
        }
        args[i] = t;
        logger.error(message + " " + "{} ".repeat(fields.size()).trim(), args);
    }

    // ========== DEBUG 레벨 ==========

    /**
     * DEBUG 로그 - 단일 필드
     */
    public static void debug(Logger logger, String message, String key, Object value) {
        if (logger.isDebugEnabled()) {
            logger.debug(message + " {}", StructuredArguments.kv(key, value));
        }
    }

    /**
     * DEBUG 로그 - 다중 필드
     */
    public static void debug(Logger logger, String message, Map<String, Object> fields) {
        if (logger.isDebugEnabled()) {
            Object[] args = fields.entrySet().stream()
                    .map(e -> StructuredArguments.kv(e.getKey(), e.getValue()))
                    .toArray();
            logger.debug(message + " " + "{} ".repeat(args.length).trim(), args);
        }
    }
}
