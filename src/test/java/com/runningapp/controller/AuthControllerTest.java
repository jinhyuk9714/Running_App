package com.runningapp.controller;

import com.runningapp.util.TestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("POST /api/auth/signup")
    class Signup {

        @Test
        @DisplayName("회원가입 성공")
        void signup_success() throws Exception {
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"signup1@test.com\",\"password\":\"password123\",\"nickname\":\"테스터1\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.user.email").value("signup1@test.com"))
                    .andExpect(jsonPath("$.user.nickname").value("테스터1"))
                    .andExpect(jsonPath("$.user.id").exists())
                    .andExpect(jsonPath("$.user.level").value(1))
                    .andExpect(jsonPath("$.user.totalDistance").value(0.0));
        }

        @Test
        @DisplayName("중복 이메일 회원가입 실패")
        void signup_duplicateEmail_fail() throws Exception {
            String body = "{\"email\":\"dup@test.com\",\"password\":\"password123\",\"nickname\":\"테스터\"}";
            mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다"));
        }

        @Test
        @DisplayName("유효성 검증 실패 - 잘못된 이메일")
        void signup_invalidEmail_fail() throws Exception {
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"invalid-email\",\"password\":\"password123\",\"nickname\":\"테스터\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("유효성 검증 실패 - 짧은 비밀번호")
        void signup_shortPassword_fail() throws Exception {
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"test@test.com\",\"password\":\"123\",\"nickname\":\"테스터\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("로그인 성공")
        void login_success() throws Exception {
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"login@test.com\",\"password\":\"password123\",\"nickname\":\"로그인테스터\"}"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"login@test.com\",\"password\":\"password123\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.user.email").value("login@test.com"))
                    .andExpect(jsonPath("$.user.nickname").value("로그인테스터"));
        }

        @Test
        @DisplayName("로그인 실패 - 존재하지 않는 이메일")
        void login_wrongEmail_fail() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"nonexistent@test.com\",\"password\":\"password123\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다"));
        }

        @Test
        @DisplayName("로그인 실패 - 잘못된 비밀번호")
        void login_wrongPassword_fail() throws Exception {
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"wrongpwd@test.com\",\"password\":\"password123\",\"nickname\":\"테스터\"}"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"wrongpwd@test.com\",\"password\":\"wrongpassword\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다"));
        }
    }

    @Nested
    @DisplayName("GET /api/auth/me")
    class GetMe {

        @Test
        @DisplayName("내 정보 조회 성공")
        void getMe_success() throws Exception {
            mockMvc.perform(post("/api/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"me@test.com\",\"password\":\"password123\",\"nickname\":\"나나\"}"))
                    .andExpect(status().isOk());

            ResultActions loginResult = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"me@test.com\",\"password\":\"password123\"}"))
                    .andExpect(status().isOk());
            String token = TestUtils.extractAccessToken(loginResult);

            mockMvc.perform(get("/api/auth/me")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("me@test.com"))
                    .andExpect(jsonPath("$.nickname").value("나나"))
                    .andExpect(jsonPath("$.id").exists());
        }

        @Test
        @DisplayName("인증 없이 조회 시 403")
        void getMe_unauthorized_fail() throws Exception {
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isForbidden());
        }
    }
}
