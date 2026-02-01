package com.runningapp.exception;

/** 400 Bad Request - 클라이언트 요청 오류 (검증 실패, 중복 등) */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
