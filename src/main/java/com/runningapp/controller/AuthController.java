package com.runningapp.controller;

import com.runningapp.dto.auth.AuthResponse;
import com.runningapp.dto.auth.LoginRequest;
import com.runningapp.dto.auth.ProfileUpdateRequest;
import com.runningapp.dto.auth.SignupRequest;
import com.runningapp.security.AuthenticationPrincipal;
import com.runningapp.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 API 컨트롤러
 *
 * @RestController = @Controller + @ResponseBody (JSON 응답)
 * @RequestMapping: 모든 메서드에 /api/auth prefix 적용
 * @Valid: 요청 바디 검증 (Jakarta Validation)
 */
@Tag(name = "인증", description = "회원가입, 로그인, 내 정보 조회 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입", description = "이메일, 비밀번호, 닉네임으로 회원가입. 인증 불필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "가입 성공, JWT 토큰 반환"),
            @ApiResponse(responseCode = "400", description = "이메일 중복 또는 유효성 검증 실패")
    })
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        AuthResponse response = authService.signup(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "로그인", description = "이메일/비밀번호로 로그인. JWT 토큰 반환.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "400", description = "이메일 또는 비밀번호 오류")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "프로필 수정", description = "닉네임, 체중, 신장 수정. 전송한 필드만 수정됨.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "유효성 검증 실패"),
            @ApiResponse(responseCode = "403", description = "인증 필요")
    })
    @PatchMapping("/me")
    public ResponseEntity<AuthResponse.UserInfo> updateProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ProfileUpdateRequest request) {
        AuthResponse.UserInfo user = authService.updateProfile(userId, request);
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "내 정보 조회", description = "JWT 토큰으로 인증된 사용자 정보 조회. Authorization 헤더에 Bearer 토큰 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "인증 실패 (토큰 없음 또는 만료)")
    })
    @GetMapping("/me")
    public ResponseEntity<AuthResponse.UserInfo> getMe(
            @AuthenticationPrincipal Long userId) {
        AuthResponse.UserInfo user = authService.getMe(userId);
        return ResponseEntity.ok(user);
    }
}
