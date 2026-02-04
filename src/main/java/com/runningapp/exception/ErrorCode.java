package com.runningapp.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드 정의
 *
 * 코드 체계:
 * - AUTH_XXX: 인증/사용자 관련
 * - ACTIVITY_XXX: 러닝 활동 관련
 * - CHALLENGE_XXX: 챌린지 관련
 * - PLAN_XXX: 트레이닝 플랜 관련
 * - COMMON_XXX: 공통 에러
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // AUTH (인증/사용자)
    AUTH_001("AUTH_001", "이미 사용 중인 이메일입니다", HttpStatus.BAD_REQUEST),
    AUTH_002("AUTH_002", "이메일 또는 비밀번호가 올바르지 않습니다", HttpStatus.BAD_REQUEST),
    AUTH_003("AUTH_003", "사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    AUTH_004("AUTH_004", "인증이 필요합니다", HttpStatus.UNAUTHORIZED),
    AUTH_005("AUTH_005", "접근 권한이 없습니다", HttpStatus.FORBIDDEN),

    // ACTIVITY (러닝 활동)
    ACTIVITY_001("ACTIVITY_001", "활동을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    ACTIVITY_002("ACTIVITY_002", "본인의 활동만 조회/수정/삭제할 수 있습니다", HttpStatus.FORBIDDEN),

    // CHALLENGE (챌린지)
    CHALLENGE_001("CHALLENGE_001", "챌린지를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    CHALLENGE_002("CHALLENGE_002", "이미 참여 중인 챌린지입니다", HttpStatus.CONFLICT),
    CHALLENGE_003("CHALLENGE_003", "참여 기간이 아닌 챌린지입니다", HttpStatus.BAD_REQUEST),
    CHALLENGE_004("CHALLENGE_004", "참여한 챌린지를 찾을 수 없습니다", HttpStatus.NOT_FOUND),

    // PLAN (트레이닝 플랜)
    PLAN_001("PLAN_001", "플랜을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    PLAN_002("PLAN_002", "이미 진행중인 플랜입니다", HttpStatus.CONFLICT),

    // COMMON (공통)
    COMMON_001("COMMON_001", "입력값 검증 실패", HttpStatus.BAD_REQUEST),
    COMMON_002("COMMON_002", "서버 내부 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR),

    // RATE_LIMIT (요청 제한)
    RATE_LIMIT_001("RATE_LIMIT_001", "요청 한도를 초과했습니다", HttpStatus.TOO_MANY_REQUESTS);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
