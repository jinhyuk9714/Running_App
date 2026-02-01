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
class TrainingPlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        String testEmail = "plan-" + UUID.randomUUID() + "@test.com";
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + testEmail + "\",\"password\":\"password123\",\"nickname\":\"플랜테스터\"}"));
        ResultActions loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + testEmail + "\",\"password\":\"password123\"}"));
        authToken = TestUtils.extractAccessToken(loginResult);
    }

    @Nested
    @DisplayName("GET /api/plans")
    class GetPlans {

        @Test
        @DisplayName("플랜 목록 조회 성공 (인증 불필요)")
        void getPlans_success() throws Exception {
            mockMvc.perform(get("/api/plans"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("목표별 플랜 목록 조회")
        void getPlans_byGoalType() throws Exception {
            mockMvc.perform(get("/api/plans").param("goalType", "FIVE_K"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/plans/recommended")
    class GetRecommendedPlans {

        @Test
        @DisplayName("추천 플랜 조회 성공")
        void getRecommended_success() throws Exception {
            mockMvc.perform(get("/api/plans/recommended")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("인증 없이 조회 시 403")
        void getRecommended_unauthorized_fail() throws Exception {
            mockMvc.perform(get("/api/plans/recommended"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/plans/{id}/start")
    class StartPlan {

        @Test
        @DisplayName("플랜 시작 성공")
        void start_success() throws Exception {
            mockMvc.perform(post("/api/plans/1/start")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.plan").exists())
                    .andExpect(jsonPath("$.currentWeek").value(1))
                    .andExpect(jsonPath("$.inProgress").value(true));
        }

        @Test
        @DisplayName("존재하지 않는 플랜 시작 시 404")
        void start_notFound_fail() throws Exception {
            mockMvc.perform(post("/api/plans/99999/start")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/plans/my")
    class GetMyPlans {

        @Test
        @DisplayName("내 플랜 목록 조회 성공")
        void getMy_success() throws Exception {
            mockMvc.perform(get("/api/plans/my")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/plans/{id}/schedule")
    class GetSchedule {

        @Test
        @DisplayName("주차별 스케줄 조회 성공 (인증 불필요)")
        void getSchedule_success() throws Exception {
            mockMvc.perform(get("/api/plans/1/schedule"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("존재하지 않는 플랜 스케줄 조회 시 404")
        void getSchedule_notFound_fail() throws Exception {
            mockMvc.perform(get("/api/plans/99999/schedule"))
                    .andExpect(status().isNotFound());
        }
    }
}
