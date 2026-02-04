package com.runningapp.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 처리기
 *
 * @RestControllerAdvice: 모든 @RestController에서 발생한 예외를 처리
 * - 애플리케이션 전체에 일관된 에러 응답 형식 제공
 * - 에러 코드 체계: AUTH_XXX, ACTIVITY_XXX, CHALLENGE_XXX, PLAN_XXX
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 비즈니스 예외 처리 (ErrorCode 포함) */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("Business exception: {} - {}", errorCode.getCode(), e.getMessage());

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode, e.getMessage()));
    }

    /** 400 Bad Request - 비즈니스 로직 오류 (중복 이메일 등) - 레거시 지원 */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException e) {
        log.warn("Bad request: {}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.COMMON_001, e.getMessage()));
    }

    /** 404 Not Found - 리소스 없음 - 레거시 지원 */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException e) {
        log.warn("Not found: {}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(e.getMessage()));
    }

    /** 400 Bad Request - @Valid 검증 실패 (이메일 형식, 비밀번호 길이 등) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(fieldName, message);
        });

        log.warn("Validation failed: {}", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.COMMON_001, errors));
    }

    /** 429 Too Many Requests - Rate Limit 초과 */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(RateLimitExceededException e) {
        log.warn("Rate limit exceeded: {}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ErrorResponse.of(ErrorCode.RATE_LIMIT_001, e.getMessage()));
    }

    /** 500 Internal Server Error - 기타 모든 예외 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected error", e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(ErrorCode.COMMON_002));
    }

    @Schema(description = "에러 응답")
    public record ErrorResponse(
            @Schema(description = "에러 코드", example = "AUTH_001")
            String code,

            @Schema(description = "에러 메시지", example = "이미 사용 중인 이메일입니다")
            String message,

            @Schema(description = "필드별 에러 (유효성 검증 실패 시)")
            Map<String, String> errors,

            @Schema(description = "발생 시각")
            LocalDateTime timestamp
    ) {
        public static ErrorResponse of(ErrorCode errorCode) {
            return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), null, LocalDateTime.now());
        }

        public static ErrorResponse of(ErrorCode errorCode, String message) {
            return new ErrorResponse(errorCode.getCode(), message, null, LocalDateTime.now());
        }

        public static ErrorResponse of(ErrorCode errorCode, Map<String, String> errors) {
            return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), errors, LocalDateTime.now());
        }

        public static ErrorResponse of(String message) {
            return new ErrorResponse(null, message, null, LocalDateTime.now());
        }

        public static ErrorResponse of(String message, Map<String, String> errors) {
            return new ErrorResponse(null, message, errors, LocalDateTime.now());
        }
    }
}
