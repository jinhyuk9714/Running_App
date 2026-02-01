package com.runningapp.controller;

import com.runningapp.dto.activity.ActivityRequest;
import com.runningapp.dto.activity.ActivityResponse;
import com.runningapp.dto.activity.ActivityStatsResponse;
import com.runningapp.security.AuthenticationPrincipal;
import com.runningapp.service.RunningActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 러닝 활동 API 컨트롤러
 *
 * 모든 메서드 authenticated - JWT 필요
 * @PageableDefault: 페이징 기본값 (size=20)
 */
@Tag(name = "러닝 활동", description = "러닝 기록 저장, 조회, 수정, 삭제, 통계 API")
@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class RunningActivityController {

    private final RunningActivityService activityService;

    @Operation(summary = "활동 저장", description = "새 러닝 활동을 저장합니다. 거리, 시간, 페이스 등 입력. GPS 경로(route)는 선택.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "저장 성공"),
            @ApiResponse(responseCode = "400", description = "유효성 검증 실패"),
            @ApiResponse(responseCode = "403", description = "인증 필요")
    })
    @PostMapping
    public ResponseEntity<ActivityResponse> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ActivityRequest request) {
        ActivityResponse response = activityService.create(userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "활동 목록 조회", description = "내 러닝 활동 목록을 페이징하여 조회. 최신순 정렬.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "인증 필요")
    })
    @GetMapping
    public ResponseEntity<Page<ActivityResponse>> getMyActivities(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ActivityResponse> activities = activityService.getMyActivities(userId, pageable);
        return ResponseEntity.ok(activities);
    }

    @Operation(summary = "활동 상세 조회", description = "특정 러닝 활동의 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "활동을 찾을 수 없음")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ActivityResponse> getActivity(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        ActivityResponse activity = activityService.getActivity(userId, id);
        return ResponseEntity.ok(activity);
    }

    @Operation(summary = "활동 수정", description = "기존 러닝 활동 정보를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "유효성 검증 실패"),
            @ApiResponse(responseCode = "403", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "활동을 찾을 수 없음")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ActivityResponse> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @Valid @RequestBody ActivityRequest request) {
        ActivityResponse activity = activityService.update(userId, id, request);
        return ResponseEntity.ok(activity);
    }

    @Operation(summary = "활동 삭제", description = "러닝 활동을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "활동을 찾을 수 없음")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        activityService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "통계 조회", description = "러닝 통계를 조회합니다. year, month 미지정 시 전체 누적 통계. year만 지정 시 해당 연도, year+month 지정 시 해당 월 통계.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "인증 필요")
    })
    @GetMapping("/stats")
    public ResponseEntity<ActivityStatsResponse> getStats(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "연도 (예: 2025)") @RequestParam(required = false) Integer year,
            @Parameter(description = "월 (1~12)") @RequestParam(required = false) Integer month) {
        ActivityStatsResponse stats = activityService.getStats(userId, year, month);
        return ResponseEntity.ok(stats);
    }
}
