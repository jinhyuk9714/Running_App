package com.runningapp.controller;

import com.runningapp.domain.GoalType;
import com.runningapp.domain.PlanDifficulty;
import com.runningapp.dto.plan.PlanResponse;
import com.runningapp.dto.plan.PlanWeekResponse;
import com.runningapp.dto.plan.UserPlanResponse;
import com.runningapp.security.AuthenticationPrincipal;
import com.runningapp.service.TrainingPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 트레이닝 플랜 API 컨트롤러
 */
@Tag(name = "트레이닝 플랜", description = "5K, 10K, 하프마라톤 목표별 플랜 API")
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class TrainingPlanController {

    private final TrainingPlanService planService;

    @Operation(summary = "플랜 목록 조회", description = "목표 유형/난이도별 플랜 목록. 인증 불필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ResponseEntity<List<PlanResponse>> getPlans(
            @Parameter(description = "목표 유형: FIVE_K, TEN_K, HALF_MARATHON") @RequestParam(required = false) GoalType goalType,
            @Parameter(description = "난이도: BEGINNER, INTERMEDIATE, ADVANCED") @RequestParam(required = false) PlanDifficulty difficulty) {
        List<PlanResponse> plans = planService.getPlans(goalType, difficulty);
        return ResponseEntity.ok(plans);
    }

    @Operation(summary = "추천 플랜", description = "사용자 레벨 기반 추천 플랜. 목표 유형 선택 가능.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "인증 필요")
    })
    @GetMapping("/recommended")
    public ResponseEntity<List<PlanResponse>> getRecommendedPlans(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "목표 유형 (선택)") @RequestParam(required = false) GoalType goalType) {
        List<PlanResponse> plans = planService.getRecommendedPlans(userId, goalType);
        return ResponseEntity.ok(plans);
    }

    @Operation(summary = "플랜 시작", description = "트레이닝 플랜을 시작합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "시작 성공"),
            @ApiResponse(responseCode = "400", description = "이미 진행중인 플랜"),
            @ApiResponse(responseCode = "403", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "플랜을 찾을 수 없음")
    })
    @PostMapping("/{id}/start")
    public ResponseEntity<UserPlanResponse> startPlan(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        UserPlanResponse response = planService.startPlan(userId, id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "내 플랜 목록", description = "내가 시작한 플랜 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "인증 필요")
    })
    @GetMapping("/my")
    public ResponseEntity<List<UserPlanResponse>> getMyPlans(
            @AuthenticationPrincipal Long userId) {
        List<UserPlanResponse> plans = planService.getMyPlans(userId);
        return ResponseEntity.ok(plans);
    }

    @Operation(summary = "주차별 스케줄", description = "플랜의 주차별 러닝 스케줄을 조회합니다. 인증 불필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "플랜을 찾을 수 없음")
    })
    @GetMapping("/{id}/schedule")
    public ResponseEntity<List<PlanWeekResponse>> getSchedule(@PathVariable Long id) {
        List<PlanWeekResponse> schedule = planService.getSchedule(id);
        return ResponseEntity.ok(schedule);
    }
}
