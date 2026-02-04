package com.runningapp.service;

import com.runningapp.config.BusinessMetrics;
import com.runningapp.domain.User;
import com.runningapp.dto.auth.AuthResponse;
import com.runningapp.dto.auth.LoginRequest;
import com.runningapp.dto.auth.ProfileUpdateRequest;
import com.runningapp.dto.auth.SignupRequest;
import com.runningapp.exception.BadRequestException;
import com.runningapp.repository.UserRepository;
import com.runningapp.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private BusinessMetrics metrics;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@test.com")
                .password("encodedPassword")
                .nickname("테스터")
                .build();
        setField(testUser, "id", 1L);
    }

    // 리플렉션으로 필드 값 설정
    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }

    // SignupRequest 생성 헬퍼
    private SignupRequest createSignupRequest(String email, String password, String nickname) {
        SignupRequest request = new SignupRequest();
        setField(request, "email", email);
        setField(request, "password", password);
        setField(request, "nickname", nickname);
        return request;
    }

    // LoginRequest 생성 헬퍼
    private LoginRequest createLoginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        setField(request, "email", email);
        setField(request, "password", password);
        return request;
    }

    // ProfileUpdateRequest 생성 헬퍼
    private ProfileUpdateRequest createProfileUpdateRequest(String nickname, Double weight, Double height) {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        if (nickname != null) setField(request, "nickname", nickname);
        if (weight != null) setField(request, "weight", weight);
        if (height != null) setField(request, "height", height);
        return request;
    }

    @Nested
    @DisplayName("signup()")
    class Signup {

        @Test
        @DisplayName("성공 - 새 사용자 회원가입")
        void signup_success() {
            // given
            SignupRequest request = createSignupRequest("new@test.com", "password123", "새사용자");

            given(userRepository.existsByEmail("new@test.com")).willReturn(false);
            given(passwordEncoder.encode("password123")).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willAnswer(invocation -> {
                User user = invocation.getArgument(0);
                setField(user, "id", 1L);
                return user;
            });
            given(jwtUtil.generateToken(anyLong(), anyString())).willReturn("test-jwt-token");

            // when
            AuthResponse response = authService.signup(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("test-jwt-token");
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            assertThat(response.getUser().getEmail()).isEqualTo("new@test.com");
            assertThat(response.getUser().getNickname()).isEqualTo("새사용자");

            verify(userRepository).existsByEmail("new@test.com");
            verify(passwordEncoder).encode("password123");
            verify(userRepository).save(any(User.class));
            verify(jwtUtil).generateToken(anyLong(), anyString());
        }

        @Test
        @DisplayName("실패 - 중복 이메일")
        void signup_duplicateEmail_throwsException() {
            // given
            SignupRequest request = createSignupRequest("existing@test.com", "password123", "테스터");

            given(userRepository.existsByEmail("existing@test.com")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> authService.signup(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("이미 사용 중인 이메일입니다");

            verify(userRepository).existsByEmail("existing@test.com");
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("성공 - 올바른 자격 증명")
        void login_success() {
            // given
            LoginRequest request = createLoginRequest("test@test.com", "password123");

            given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);
            given(jwtUtil.generateToken(1L, "test@test.com")).willReturn("test-jwt-token");

            // when
            AuthResponse response = authService.login(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("test-jwt-token");
            assertThat(response.getUser().getEmail()).isEqualTo("test@test.com");

            verify(userRepository).findByEmail("test@test.com");
            verify(passwordEncoder).matches("password123", "encodedPassword");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 이메일")
        void login_emailNotFound_throwsException() {
            // given
            LoginRequest request = createLoginRequest("nonexistent@test.com", "password123");

            given(userRepository.findByEmail("nonexistent@test.com")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다");

            verify(userRepository).findByEmail("nonexistent@test.com");
            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("실패 - 잘못된 비밀번호")
        void login_wrongPassword_throwsException() {
            // given
            LoginRequest request = createLoginRequest("test@test.com", "wrongPassword");

            given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다");

            verify(passwordEncoder).matches("wrongPassword", "encodedPassword");
            verify(jwtUtil, never()).generateToken(anyLong(), anyString());
        }
    }

    @Nested
    @DisplayName("getMe()")
    class GetMe {

        @Test
        @DisplayName("성공 - 사용자 정보 조회")
        void getMe_success() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

            // when
            AuthResponse.UserInfo userInfo = authService.getMe(1L);

            // then
            assertThat(userInfo).isNotNull();
            assertThat(userInfo.getId()).isEqualTo(1L);
            assertThat(userInfo.getEmail()).isEqualTo("test@test.com");
            assertThat(userInfo.getNickname()).isEqualTo("테스터");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사용자")
        void getMe_userNotFound_throwsException() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.getMe(999L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("사용자를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("updateProfile()")
    class UpdateProfile {

        @Test
        @DisplayName("성공 - 프로필 수정")
        void updateProfile_success() {
            // given
            ProfileUpdateRequest request = createProfileUpdateRequest("수정된닉네임", 70.0, 175.0);

            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willReturn(testUser);

            // when
            AuthResponse.UserInfo userInfo = authService.updateProfile(1L, request);

            // then
            assertThat(userInfo).isNotNull();
            verify(userRepository).findById(1L);
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사용자")
        void updateProfile_userNotFound_throwsException() {
            // given
            ProfileUpdateRequest request = createProfileUpdateRequest("새닉네임", null, null);

            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.updateProfile(999L, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("사용자를 찾을 수 없습니다");
        }
    }
}
