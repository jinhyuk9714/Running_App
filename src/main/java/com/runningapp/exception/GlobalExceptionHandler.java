package com.runningapp.exception;

import io.swagger.v3.oas.annotations.media.Schema;
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
 * - 4xx, 5xx HTTP 상태 코드 + JSON 메시지
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 400 Bad Request - 비즈니스 로직 오류 (중복 이메일 등) */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(e.getMessage()));
    }

    /** 404 Not Found - 리소스 없음 */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException e) {
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
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("입력값 검증 실패", errors));
    }

    /** 500 Internal Server Error - 기타 모든 예외 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        e.printStackTrace();
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("서버 오류가 발생했습니다"));
    }

    @Schema(description = "에러 응답")
    public record ErrorResponse(
            @Schema(description = "에러 메시지") String message,
            @Schema(description = "필드별 에러 (유효성 검증 실패 시)") Map<String, String> errors,
            @Schema(description = "발생 시각") LocalDateTime timestamp
    ) {
        public static ErrorResponse of(String message) {
            return new ErrorResponse(message, null, LocalDateTime.now());
        }

        public static ErrorResponse of(String message, Map<String, String> errors) {
            return new ErrorResponse(message, errors, LocalDateTime.now());
        }
    }
}
