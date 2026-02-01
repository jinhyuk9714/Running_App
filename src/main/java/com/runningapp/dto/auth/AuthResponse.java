package com.runningapp.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * 인증 응답 DTO (로그인/회원가입 응답)
 *
 * JWT 토큰 + 사용자 정보를 함께 반환
 * UserInfo: 중첩 클래스로 사용자 정보만 필요한 경우 재사용 (GET /me)
 */
@Schema(description = "인증 응답 (JWT 토큰 + 사용자 정보)")
@Getter
@Builder
public class AuthResponse {

    @Schema(description = "JWT 액세스 토큰")
    private String accessToken;

    @Schema(description = "토큰 타입 (Bearer)")
    private String tokenType;

    @Schema(description = "사용자 정보")
    private UserInfo user;

    @Schema(description = "사용자 정보")
    @Getter
    @Builder
    public static class UserInfo {
        @Schema(description = "사용자 ID")
        private Long id;
        @Schema(description = "이메일")
        private String email;
        @Schema(description = "닉네임")
        private String nickname;
        @Schema(description = "레벨")
        private Integer level;
        @Schema(description = "누적 러닝 거리 (km)")
        private Double totalDistance;
    }
}
