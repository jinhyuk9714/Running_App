package com.runningapp.controller;

import com.runningapp.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ChallengeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        String testEmail = "challenge-" + UUID.randomUUID() + "@test.com";
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + testEmail + "\",\"password\":\"password123\",\"nickname\":\"챌린지테스터\"}"));
        ResultActions loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + testEmail + "\",\"password\":\"password123\"}"));
        authToken = TestUtils.extractAccessToken(loginResult);
    }

    @Nested
    @DisplayName("GET /api/challenges")
    class GetActiveChallenges {

        @Test
        @DisplayName("진행중인 챌린지 목록 조회 성공 (인증 불필요)")
        void getActiveChallenges_success() throws Exception {
            mockMvc.perform(get("/api/challenges"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/challenges/recommended")
    class GetRecommendedChallenges {

        @Test
        @DisplayName("추천 챌린지 조회 성공")
        void getRecommended_success() throws Exception {
            mockMvc.perform(get("/api/challenges/recommended")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("인증 없이 조회 시 403")
        void getRecommended_unauthorized_fail() throws Exception {
            mockMvc.perform(get("/api/challenges/recommended"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/challenges/{id}/join")
    class JoinChallenge {

        @Test
        @DisplayName("챌린지 참여 성공")
        void join_success() throws Exception {
            // 시드 데이터로 챌린지 1, 2가 생성됨
            mockMvc.perform(post("/api/challenges/1/join")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.challenge").exists())
                    .andExpect(jsonPath("$.currentDistance").value(0.0))
                    .andExpect(jsonPath("$.progressPercent").exists());
        }

        @Test
        @DisplayName("존재하지 않는 챌린지 참여 시 404")
        void join_notFound_fail() throws Exception {
            mockMvc.perform(post("/api/challenges/99999/join")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/challenges/my")
    class GetMyChallenges {

        @Test
        @DisplayName("내 참여 챌린지 목록 조회 성공")
        void getMy_success() throws Exception {
            mockMvc.perform(get("/api/challenges/my")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }
}
