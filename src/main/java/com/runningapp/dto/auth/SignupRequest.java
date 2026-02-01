package com.runningapp.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원가입 요청 DTO (Data Transfer Object)
 *
 * DTO: 계층 간 데이터 전달 객체. Entity를 그대로 노출하지 않아 보안·유연성 확보
 * Jakarta Validation: @Valid와 함께 요청 바디 검증. 실패 시 MethodArgumentNotValidException
 */
@Schema(description = "회원가입 요청")
@Getter
@NoArgsConstructor
public class SignupRequest {

    @Schema(description = "이메일", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    @Schema(description = "비밀번호 (8자 이상)", example = "password123", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 8)
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
    private String password;

    @Schema(description = "닉네임 (2~20자)", example = "러너", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 2, maxLength = 20)
    @NotBlank(message = "닉네임은 필수입니다")
    @Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다")
    private String nickname;
}
