package com.runningapp.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "프로필 수정 요청")
@Getter
@NoArgsConstructor
public class ProfileUpdateRequest {

    @Schema(description = "닉네임 (2~20자, 선택)", example = "러너", minLength = 2, maxLength = 20)
    @Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다")
    private String nickname;  // null이면 수정 안 함

    @Schema(description = "체중 (kg)", example = "70.0")
    private Double weight;

    @Schema(description = "신장 (cm)", example = "175.0")
    private Double height;
}
