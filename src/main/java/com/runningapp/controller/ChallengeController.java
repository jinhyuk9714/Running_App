package com.runningapp.controller;

import com.runningapp.dto.challenge.ChallengeResponse;
import com.runningapp.dto.challenge.UserChallengeResponse;
import com.runningapp.security.AuthenticationPrincipal;
import com.runningapp.service.ChallengeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 챌린지 API 컨트롤러
 */
@Tag(name = "챌린지", description = "챌린지 목록, 참여, 진행률 조회 API")
@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;

    @Operation(summary = "진행중인 챌린지 목록", description = "현재 진행중인 모든 챌린지를 조회합니다. 인증 불필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ResponseEntity<List<ChallengeResponse>> getActiveChallenges() {
        List<ChallengeResponse> challenges = challengeService.getActiveChallenges();
        return ResponseEntity.ok(challenges);
    }

    @Operation(summary = "추천 챌린지", description = "진행중인 챌린지 중 아직 참여하지 않은 챌린지를 추천합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "인증 필요")
    })
    @GetMapping("/recommended")
    public ResponseEntity<List<ChallengeResponse>> getRecommendedChallenges(
            @AuthenticationPrincipal Long userId) {
        List<ChallengeResponse> challenges = challengeService.getRecommendedChallenges(userId);
        return ResponseEntity.ok(challenges);
    }

    @Operation(summary = "챌린지 참여", description = "챌린지에 참여합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "참여 성공"),
            @ApiResponse(responseCode = "400", description = "이미 참여중이거나 참여 기간 아님"),
            @ApiResponse(responseCode = "403", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "챌린지를 찾을 수 없음")
    })
    @PostMapping("/{id}/join")
    public ResponseEntity<UserChallengeResponse> joinChallenge(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        UserChallengeResponse response = challengeService.joinChallenge(userId, id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "내 참여 챌린지 목록", description = "내가 참여한 챌린지 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "인증 필요")
    })
    @GetMapping("/my")
    public ResponseEntity<List<UserChallengeResponse>> getMyChallenges(
            @AuthenticationPrincipal Long userId) {
        List<UserChallengeResponse> challenges = challengeService.getMyChallenges(userId);
        return ResponseEntity.ok(challenges);
    }

    @Operation(summary = "챌린지 진행률 조회", description = "특정 챌린지의 내 진행률을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "참여한 챌린지를 찾을 수 없음")
    })
    @GetMapping("/{id}/progress")
    public ResponseEntity<UserChallengeResponse> getChallengeProgress(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        UserChallengeResponse response = challengeService.getChallengeProgress(userId, id);
        return ResponseEntity.ok(response);
    }
}
