package com.runningapp.exception;

/**
 * Rate Limit 초과 예외
 *
 * HTTP 429 Too Many Requests 응답과 함께 반환됨
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
