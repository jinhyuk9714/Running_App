package com.runningapp.service;

import com.runningapp.domain.User;
import com.runningapp.dto.auth.AuthResponse;
import com.runningapp.dto.auth.LoginRequest;
import com.runningapp.dto.auth.ProfileUpdateRequest;
import com.runningapp.dto.auth.SignupRequest;
import com.runningapp.exception.BadRequestException;
import com.runningapp.repository.UserRepository;
import com.runningapp.util.JwtUtil;
import com.runningapp.util.LogUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 인증 서비스 (회원가입, 로그인, 내 정보)
 *
 * @Service: Spring Bean으로 등록, 비즈니스 로직 담당
 * @Transactional(readOnly = true): 기본은 읽기 전용. 쓰기 작업은 @Transactional 별도 지정
 * @RequiredArgsConstructor: final 필드에 대한 생성자 주입 (의존성 주입)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional  // 쓰기 작업이므로 별도 트랜잭션 (readOnly=false)
    public AuthResponse signup(SignupRequest request) {
        // 1. 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            LogUtils.warn(log, "회원가입 실패 - 이메일 중복", "email", request.getEmail());
            throw new BadRequestException("이미 사용 중인 이메일입니다");
        }

        // 2. 비밀번호 암호화 후 사용자 저장 (평문 비밀번호 절대 저장 금지)
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .build();

        user = userRepository.save(user);

        // 3. JWT 토큰 생성하여 반환
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        LogUtils.info(log, "회원가입 성공", Map.of(
                "userId", user.getId(),
                "email", user.getEmail()
        ));

        return buildAuthResponse(token, user);
    }

    public AuthResponse login(LoginRequest request) {
        // 1. 이메일로 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    LogUtils.warn(log, "로그인 실패 - 존재하지 않는 이메일", "email", request.getEmail());
                    return new BadRequestException("이메일 또는 비밀번호가 올바르지 않습니다");
                });

        // 2. 비밀번호 검증 (BCrypt.matches: 입력 비밀번호와 해시 비교)
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            LogUtils.warn(log, "로그인 실패 - 비밀번호 불일치", Map.of(
                    "userId", user.getId(),
                    "email", user.getEmail()
            ));
            throw new BadRequestException("이메일 또는 비밀번호가 올바르지 않습니다");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        LogUtils.info(log, "로그인 성공", Map.of(
                "userId", user.getId(),
                "email", user.getEmail()
        ));

        return buildAuthResponse(token, user);
    }

    @Transactional
    public AuthResponse.UserInfo updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("사용자를 찾을 수 없습니다"));

        user.updateProfile(request.getNickname(), request.getWeight(), request.getHeight());
        userRepository.save(user);

        return AuthResponse.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .level(user.getLevel())
                .totalDistance(user.getTotalDistance())
                .build();
    }

    public AuthResponse.UserInfo getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("사용자를 찾을 수 없습니다"));

        return AuthResponse.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .level(user.getLevel())
                .totalDistance(user.getTotalDistance())
                .build();
    }

    private AuthResponse buildAuthResponse(String token, User user) {
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .nickname(user.getNickname())
                        .level(user.getLevel())
                        .totalDistance(user.getTotalDistance())
                        .build())
                .build();
    }
}
