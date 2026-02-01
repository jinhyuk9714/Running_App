package com.runningapp.controller;

import com.runningapp.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RunningActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private String authToken;
    private String testEmail;

    @BeforeEach
    void setUp() throws Exception {
        testEmail = "activity-" + UUID.randomUUID() + "@test.com";
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + testEmail + "\",\"password\":\"password123\",\"nickname\":\"활동테스터\"}"));
        ResultActions loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + testEmail + "\",\"password\":\"password123\"}"));
        authToken = TestUtils.extractAccessToken(loginResult);
    }

    private String activityJson(double distance, int duration, Integer averagePace, Integer calories,
                                String startedAt, String memo) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"distance\":").append(distance).append(",\"duration\":").append(duration);
        if (averagePace != null) sb.append(",\"averagePace\":").append(averagePace);
        if (calories != null) sb.append(",\"calories\":").append(calories);
        sb.append(",\"startedAt\":\"").append(startedAt).append("\"");
        if (memo != null) sb.append(",\"memo\":\"").append(memo).append("\"");
        sb.append("}");
        return sb.toString();
    }

    @Nested
    @DisplayName("POST /api/activities")
    class Create {

        @Test
        @DisplayName("활동 저장 성공")
        void create_success() throws Exception {
            String body = activityJson(5.2, 1800, 346, 300, "2025-02-01T07:00:00", null);

            mockMvc.perform(post("/api/activities")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.distance").value(5.2))
                    .andExpect(jsonPath("$.duration").value(1800))
                    .andExpect(jsonPath("$.averagePace").value(346))
                    .andExpect(jsonPath("$.calories").value(300))
                    .andExpect(jsonPath("$.startedAt").value("2025-02-01T07:00:00"))
                    .andExpect(jsonPath("$.createdAt").exists());
        }

        @Test
        @DisplayName("인증 없이 저장 시 403")
        void create_unauthorized_fail() throws Exception {
            String body = activityJson(5.2, 1800, 346, 300, "2025-02-01T07:00:00", null);

            mockMvc.perform(post("/api/activities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("유효성 검증 실패 - 거리 0")
        void create_invalidDistance_fail() throws Exception {
            String body = activityJson(0, 1800, 346, 300, "2025-02-01T07:00:00", null);

            mockMvc.perform(post("/api/activities")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/activities")
    class GetList {

        @Test
        @DisplayName("활동 목록 조회 성공")
        void getList_success() throws Exception {
            String body = activityJson(5.2, 1800, 346, 300, "2025-02-01T07:00:00", null);
            mockMvc.perform(post("/api/activities")
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body));

            mockMvc.perform(get("/api/activities")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].distance").value(5.2))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("인증 없이 조회 시 403")
        void getList_unauthorized_fail() throws Exception {
            mockMvc.perform(get("/api/activities"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/activities/{id}")
    class GetOne {

        @Test
        @DisplayName("활동 상세 조회 성공")
        void getOne_success() throws Exception {
            String body = activityJson(3.5, 1200, 343, 200, "2025-02-01T08:00:00", "아침 러닝");
            ResultActions createResult = mockMvc.perform(post("/api/activities")
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body));
            Long id = Long.parseLong(createResult.andReturn().getResponse().getContentAsString()
                    .split("\"id\":")[1].split(",")[0]);

            mockMvc.perform(get("/api/activities/" + id)
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.distance").value(3.5))
                    .andExpect(jsonPath("$.memo").value("아침 러닝"));
        }

        @Test
        @DisplayName("존재하지 않는 활동 조회 시 404")
        void getOne_notFound_fail() throws Exception {
            mockMvc.perform(get("/api/activities/99999")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/activities/{id}")
    class Update {

        @Test
        @DisplayName("활동 수정 성공")
        void update_success() throws Exception {
            String createBody = activityJson(5.0, 1500, 300, 250, "2025-02-01T07:00:00", "처음");
            ResultActions createResult = mockMvc.perform(post("/api/activities")
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createBody));
            Long id = Long.parseLong(createResult.andReturn().getResponse().getContentAsString()
                    .split("\"id\":")[1].split(",")[0]);

            String updateBody = activityJson(6.0, 1800, 300, 300, "2025-02-01T07:00:00", "수정됨");

            mockMvc.perform(put("/api/activities/" + id)
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.distance").value(6.0))
                    .andExpect(jsonPath("$.memo").value("수정됨"));
        }

        @Test
        @DisplayName("존재하지 않는 활동 수정 시 404")
        void update_notFound_fail() throws Exception {
            String body = activityJson(5.0, 1500, 300, 250, "2025-02-01T07:00:00", null);

            mockMvc.perform(put("/api/activities/99999")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/activities/{id}")
    class Delete {

        @Test
        @DisplayName("활동 삭제 성공")
        void delete_success() throws Exception {
            String body = activityJson(2.0, 600, 300, 100, "2025-02-01T09:00:00", null);
            ResultActions createResult = mockMvc.perform(post("/api/activities")
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body));
            Long id = Long.parseLong(createResult.andReturn().getResponse().getContentAsString()
                    .split("\"id\":")[1].split(",")[0]);

            mockMvc.perform(delete("/api/activities/" + id)
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/activities/" + id)
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("존재하지 않는 활동 삭제 시 404")
        void delete_notFound_fail() throws Exception {
            mockMvc.perform(delete("/api/activities/99999")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/activities/stats")
    class GetStats {

        @Test
        @DisplayName("통계 조회 - 전체")
        void getStats_all_success() throws Exception {
            mockMvc.perform(get("/api/activities/stats")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalDistance").exists())
                    .andExpect(jsonPath("$.totalCount").exists());
        }

        @Test
        @DisplayName("통계 조회 - 연도/월 지정")
        void getStats_withYearMonth_success() throws Exception {
            String body = activityJson(5.2, 1800, 346, 300, "2025-02-01T07:00:00", null);
            mockMvc.perform(post("/api/activities")
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body));

            mockMvc.perform(get("/api/activities/stats")
                            .header("Authorization", "Bearer " + authToken)
                            .param("year", "2025")
                            .param("month", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalDistance").value(5.2))
                    .andExpect(jsonPath("$.totalCount").value(1))
                    .andExpect(jsonPath("$.totalDuration").value(1800));
        }

        @Test
        @DisplayName("인증 없이 통계 조회 시 403")
        void getStats_unauthorized_fail() throws Exception {
            mockMvc.perform(get("/api/activities/stats"))
                    .andExpect(status().isForbidden());
        }
    }
}
