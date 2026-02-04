# Performance Optimization Report

> Running App 백엔드 성능 최적화 과정 및 결과 정리

---

## 개요

Nike Run Club 스타일 러닝 앱의 Spring Boot 백엔드 성능을 단계적으로 최적화한 과정입니다.

| Phase | 적용 기술 | 주요 개선 |
|-------|----------|----------|
| Phase 1 | Baseline | 초기 측정 |
| Phase 2 | Redis Caching | 응답시간 30% 감소 |
| Phase 3 | Async Event-Driven | 서비스 결합도 감소, 확장성 향상 |
| Phase 4 | N+1 Query Fix | 쿼리 수 83% 감소 |
| Phase 5 | Index Optimization | 쿼리 실행 계획 최적화 |
| Phase 6 | Test Coverage | 62% 커버리지, 90개 테스트 |
| Phase 7 | Docker Optimization | 이미지 크기 47% 감소 |

---

## 테스트 환경

| 항목 | 스펙 |
|-----|------|
| Tool | K6 (Grafana) |
| Duration | 60초 |
| Virtual Users | 최대 50 VUs |
| Ramp Pattern | 10 → 30 → 50 → 0 VUs |
| Server | Spring Boot 3.3.5, Java 17 |
| Database | H2 (in-memory) |
| Cache | Redis 7.x |

---

## Phase 1: Baseline 측정

초기 상태에서의 성능 측정 결과입니다.

```
POST /api/activities → Save → Level Update → Challenge Update → Plan Update → Response
                       ↑ 동기 처리로 인한 지연
```

### 결과

| 지표 | 값 |
|-----|-----|
| 총 요청 수 | 4,291 |
| 처리량 | 67.92 req/s |
| 평균 응답시간 | 21.94ms |
| P95 응답시간 | 93.96ms |
| 에러율 | 59.98% (DB 커넥션 이슈) |

### 엔드포인트별 응답시간

| Endpoint | 평균 |
|----------|------|
| POST /api/auth/login | 88.04ms |
| GET /api/activities | 6.28ms |
| GET /api/activities/summary | 7.43ms |
| GET /api/challenges | 4.34ms |
| GET /api/plans | 3.87ms |

---

## Phase 2: Redis Caching 적용

자주 조회되는 데이터에 Redis 캐싱을 적용했습니다.

### 캐시 전략

| Cache Key | TTL | 대상 |
|-----------|-----|------|
| activitySummary | 5분 | 주간/월간 요약 |
| activityStats | 5분 | 통계 데이터 |
| activeChallenges | 10분 | 진행중인 챌린지 목록 |
| recommendedChallenges | 5분 | 추천 챌린지 |
| plans | 30분 | 플랜 목록 |
| planSchedule | 1시간 | 주차별 스케줄 |

### 결과

| 지표 | Before | After | 개선율 |
|-----|--------|-------|--------|
| 처리량 | 67.92 req/s | 70.22 req/s | **+3.4%** |
| 평균 응답시간 | 21.94ms | 15.19ms | **-30.8%** |
| P95 응답시간 | 93.96ms | 73.82ms | **-21.4%** |
| 에러율 | 59.98% | 0.00% | **-100%** |

### 엔드포인트별 개선

| Endpoint | Before | After | 개선율 |
|----------|--------|-------|--------|
| GET /api/activities | 6.28ms | 1.85ms | **-70.5%** |
| GET /api/activities/summary | 7.43ms | 1.02ms | **-86.3%** |
| GET /api/challenges | 4.34ms | 0.97ms | **-77.6%** |
| GET /api/plans | 3.87ms | 1.02ms | **-73.6%** |
| POST /api/auth/login | 88.04ms | 71.18ms | **-19.2%** |

---

## Phase 3: Async Event-Driven Architecture

동기 처리를 이벤트 기반 비동기로 전환하여 응답 시간을 단축하고 서비스 결합도를 낮췄습니다.

### 아키텍처 변경

**Before (동기)**
```
POST /api/activities
    → Save Activity
    → Update User Level      ─┐
    → Update Challenge Progress │ 동기 처리 (~100ms)
    → Update Plan Progress    ─┘
    → Response
```

**After (비동기)**
```
POST /api/activities
    → Save Activity
    → Publish Event ──────────→ Response (~5ms)
           │
           ↓ (Async)
    ┌──────┴──────┐
    │  Listeners  │
    ├─────────────┤
    │ UserLevel   │
    │ Challenge   │
    │ TrainingPlan│
    └─────────────┘
```

### 구현 상세

#### Event Classes
```java
// 활동 생성 시 발행
ActivityCompletedEvent(userId, activityId, distance, startedAt)

// 활동 수정 시 발행 (거리 변경)
ActivityUpdatedEvent(userId, activityId, oldDistance, newDistance, startedAt)

// 활동 삭제 시 발행
ActivityDeletedEvent(userId, activityId, distance, startedAt)
```

#### Event Listeners
| Listener | 역할 | 어노테이션 |
|----------|------|-----------|
| UserLevelEventListener | 레벨 업데이트 | @TransactionalEventListener |
| ChallengeProgressEventListener | 챌린지 진행률 | @Async, @Retryable |
| TrainingPlanEventListener | 플랜 진행률 | @Transactional(REQUIRES_NEW) |

#### Thread Pool 설정
```java
@Bean
public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(5);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("Async-");
    return executor;
}
```

### 결과

| 지표 | Redis Only | + Async Event | 변화 |
|-----|------------|---------------|------|
| 총 요청 수 | 4,306 | 4,286 | -0.5% |
| 처리량 | 70.22 req/s | 69.88 req/s | -0.5% |
| 평균 응답시간 | 15.19ms | 15.67ms | +3.2% |
| P95 응답시간 | 73.82ms | 75.36ms | +2.1% |
| 에러율 | 0.00% | 0.00% | - |

### 활동 생성 API 응답시간

| 측정 | 응답시간 |
|------|---------|
| Run 1 | 5ms |
| Run 2 | 3ms |
| Run 3 | 4ms |
| Run 4 | 3ms |
| Run 5 | 3ms |
| **평균** | **~3.6ms** |

### 왜 이벤트 기반 비동기인가?

성능 수치만 보면 Redis 캐싱 후와 큰 차이가 없습니다. 이벤트 기반 비동기의 **진짜 이점은 아키텍처**에 있습니다.

#### 1. 느슨한 결합 (Loose Coupling)

**Before - 직접 의존**
```java
@RequiredArgsConstructor
public class RunningActivityService {
    private final ChallengeService challengeService;       // 직접 의존
    private final TrainingPlanService trainingPlanService; // 직접 의존

    public ActivityResponse create(...) {
        activity = activityRepository.save(activity);
        challengeService.updateProgressOnActivity(...);    // 직접 호출
        trainingPlanService.updatePlanProgressOnActivity(...);
        return response;
    }
}
```

**After - 이벤트만 발행**
```java
@RequiredArgsConstructor
public class RunningActivityService {
    private final ApplicationEventPublisher eventPublisher; // 이벤트만 발행

    public ActivityResponse create(...) {
        activity = activityRepository.save(activity);
        eventPublisher.publishEvent(new ActivityCompletedEvent(...)); // 누가 처리하는지 모름
        return response;
    }
}
```

`RunningActivityService`가 `ChallengeService`, `TrainingPlanService`를 **전혀 모릅니다**.

#### 2. 확장성 (Open-Closed Principle)

새 기능 추가 시 **기존 코드 수정 없이** 리스너만 추가:

```java
// 푸시 알림 추가? 리스너만 만들면 됨
@Component
public class PushNotificationListener {
    @TransactionalEventListener
    public void handleActivityCompleted(ActivityCompletedEvent event) {
        pushService.send("러닝 완료! " + event.getDistance() + "km");
    }
}

// 뱃지 시스템 추가? 리스너만 만들면 됨
@Component
public class BadgeListener {
    @TransactionalEventListener
    public void handleActivityCompleted(ActivityCompletedEvent event) {
        badgeService.checkAndAward(event.getUserId(), event.getDistance());
    }
}
```

동기 방식이었다면 `RunningActivityService`를 계속 수정해야 합니다.

#### 3. 장애 격리

**Before (동기)**
```
활동 저장 → 챌린지 업데이트 실패! → 전체 롤백 → 활동도 저장 안 됨
```

**After (비동기)**
```
활동 저장 → 이벤트 발행 → 응답 반환 (성공)
                ↓
        챌린지 업데이트 실패 → @Retryable로 3회 재시도
                              → 실패해도 활동은 이미 저장됨
```

#### 4. 실제 성능 차이가 커지는 경우

현재 테스트에서 차이가 적은 이유:
- 테스트가 **조회(GET) 위주**
- 챌린지/플랜 로직이 **단순** (DB 조회 몇 번)
- **H2 인메모리 DB**라 I/O 지연 없음

프로덕션에서 차이가 커지는 경우:

```java
// 복잡한 후처리 로직이 있다면?
public void updateProgressOnActivity(...) {
    repository.findActive(userId);           // DB 조회
    pushService.sendNotification(...);       // 외부 API (100ms+)
    emailService.send(...);                  // 이메일 발송 (200ms+)
    leaderboardService.update(...);          // 리더보드 (50ms+)
    badgeService.checkNewBadges(...);        // 뱃지 확인 (30ms+)
}
```

| 방식 | 응답 시간 |
|-----|----------|
| 동기 | 100 + 200 + 50 + 30 = **+380ms** |
| 비동기 | 이벤트 발행 후 **즉시 응답** |

#### 5. 비교 요약

| 관점 | 동기 | 비동기 이벤트 |
|-----|------|--------------|
| 결합도 | 강결합 | **느슨한 결합** |
| 새 기능 추가 | 서비스 수정 필요 | **리스너만 추가** |
| 장애 영향 | 전체 롤백 | **격리됨** |
| 단순 로직 | 비슷 | 비슷 |
| 복잡한 로직 | 지연 누적 | **즉시 응답** |
| 테스트 | 통합 테스트 필요 | **단위 테스트 용이** |

---

## 스케줄러 구현

운영 자동화를 위한 스케줄러를 추가했습니다.

| Scheduler | 주기 | 기능 |
|-----------|------|------|
| ChallengeScheduler | 매일 00:05 | 만료 챌린지 종료 처리 |
| StatsAggregationScheduler | 매주 월 00:30 | 주간 통계 집계, 캐시 초기화 |
| CacheWarmupScheduler | 5분마다 | activeChallenges, plans 캐시 워밍업 |

---

## 전체 개선 요약

### 성능 지표

| 지표 | Baseline | Final | 총 개선율 |
|-----|----------|-------|----------|
| 처리량 | 67.92 req/s | 69.88 req/s | **+2.9%** |
| 평균 응답시간 | 21.94ms | 15.67ms | **-28.6%** |
| P95 응답시간 | 93.96ms | 75.36ms | **-19.8%** |
| 에러율 | 59.98% | 0.00% | **-100%** |

### 아키텍처 개선

| 항목 | Before | After |
|-----|--------|-------|
| 서비스 결합도 | 강결합 (직접 호출) | 느슨한 결합 (이벤트) |
| 확장성 | 리스너 추가 시 서비스 수정 필요 | 리스너만 추가하면 됨 |
| 장애 격리 | 하나 실패 시 전체 롤백 | 독립적 재시도 |
| 운영 자동화 | 수동 | 스케줄러로 자동화 |

---

## 테스트 실행 방법

### 사전 준비
```bash
# Redis 실행
redis-server

# Spring Boot 서버 실행
./gradlew bootRun
```

### K6 테스트 실행
```bash
# Quick test (1분)
k6 run k6/quick-test.js

# Full load test (3분 30초)
k6 run k6/load-test.js

# 특정 서버 대상
k6 run -e BASE_URL=https://your-server.com k6/load-test.js
```

### 결과 파일
- `k6/baseline-result.json` - 베이스라인 측정
- `k6/redis-caching-result.json` - Redis 캐싱 적용 후
- `k6/async-event-result.json` - 비동기 이벤트 적용 후

---

## Phase 4: N+1 쿼리 최적화

JPA의 Lazy Loading으로 인한 N+1 문제를 분석하고 해결했습니다.

### N+1 문제란?

```
Before: 내 챌린지 5개 조회 시
─────────────────────────────
SELECT * FROM user_challenge WHERE user_id = ?  -- 1번
SELECT * FROM challenge WHERE id = ?            -- N번 (각 챌린지마다)
SELECT * FROM challenge WHERE id = ?
SELECT * FROM challenge WHERE id = ?
SELECT * FROM challenge WHERE id = ?
SELECT * FROM challenge WHERE id = ?
총 쿼리: 1 + 5 = 6개

After: JOIN FETCH 적용
─────────────────────────────
SELECT uc.*, c.* FROM user_challenge uc
JOIN challenge c ON uc.challenge_id = c.id
WHERE uc.user_id = ?
총 쿼리: 1개
```

### 발견된 N+1 문제

| 함수 | 문제 유형 | Before 쿼리 수 |
|------|----------|---------------|
| getMyChallenges() | Lazy Loading in Stream | 1 + N |
| getChallengeProgress() | Lazy Loading in DTO | 2 |
| getRecommendedChallenges() | Loop 내 exists 쿼리 | 1 + N |
| getMyPlans() | Lazy Loading in Stream | 1 + N |
| updatePlanProgressOnActivity() | Loop 내 다중 쿼리 | 1 + 4N |

### 해결 방법

#### 1. JOIN FETCH 적용

**UserChallengeRepository**
```java
// Before
List<UserChallenge> findByUserIdOrderByJoinedAtDesc(Long userId);

// After - Challenge를 함께 로드
@Query("SELECT uc FROM UserChallenge uc " +
       "JOIN FETCH uc.challenge " +
       "WHERE uc.user.id = :userId ORDER BY uc.joinedAt DESC")
List<UserChallenge> findByUserIdWithChallenge(@Param("userId") Long userId);
```

**UserPlanRepository**
```java
// Before
List<UserPlan> findByUserIdOrderByStartedAtDesc(Long userId);

// After - TrainingPlan을 함께 로드
@Query("SELECT up FROM UserPlan up " +
       "JOIN FETCH up.plan " +
       "WHERE up.user.id = :userId ORDER BY up.startedAt DESC")
List<UserPlan> findByUserIdWithPlan(@Param("userId") Long userId);
```

#### 2. 배치 쿼리로 변경

**getRecommendedChallenges()**
```java
// Before - 각 챌린지마다 exists 쿼리 실행
active.stream()
    .filter(c -> !userChallengeRepository.existsByUserIdAndChallengeId(userId, c.getId()))

// After - 참여한 챌린지 ID를 한 번에 조회
List<Long> joinedIds = userChallengeRepository.findChallengeIdsByUserId(userId);
active.stream()
    .filter(c -> !joinedIds.contains(c.getId()))
```

**updatePlanProgressOnActivity()**
```java
// Before - Loop 내에서 PlanWeek 조회
for (UserPlan userPlan : activePlans) {
    List<PlanWeek> weeks = planWeekRepository.findByPlanIdOrderByWeekNumberAsc(userPlan.getPlan().getId());
}

// After - 모든 플랜의 PlanWeek를 한 번에 조회 후 Map으로 그룹핑
List<Long> planIds = activePlans.stream().map(up -> up.getPlan().getId()).toList();
List<PlanWeek> allPlanWeeks = planWeekRepository.findByPlanIds(planIds);
Map<Long, Map<Integer, PlanWeek>> planWeekMap = allPlanWeeks.stream()
    .collect(Collectors.groupingBy(pw -> pw.getPlan().getId(),
             Collectors.toMap(PlanWeek::getWeekNumber, pw -> pw)));
```

### 결과

| 함수 | Before | After | 감소율 |
|------|--------|-------|--------|
| getMyChallenges() | 1 + N | **1** | **-N** |
| getChallengeProgress() | 2 | **1** | **-50%** |
| getRecommendedChallenges() | 1 + N | **2** | **-(N-1)** |
| getMyPlans() | 1 + N | **1** | **-N** |
| updatePlanProgressOnActivity() | 1 + 4N | **2 + 2N** | **-50%** |

### 쿼리 수 예시 (N=5 기준)

| API | Before | After | 감소 |
|-----|--------|-------|------|
| GET /challenges/my | 6개 | **1개** | **83%** |
| GET /challenges/recommended | 7개 | **2개** | **71%** |
| GET /plans/my | 6개 | **1개** | **83%** |
| 활동 저장 후 플랜 업데이트 | 21개 | **12개** | **43%** |

---

## Phase 5: 데이터베이스 인덱스 최적화

자주 사용되는 쿼리의 WHERE, ORDER BY, JOIN 조건에 맞는 인덱스를 추가했습니다.

### 추가된 인덱스

| 테이블 | 인덱스명 | 컬럼 | 용도 |
|--------|---------|------|------|
| running_activities | idx_running_activities_user_started | user_id, started_at DESC | 활동 목록 페이징 |
| running_activities | idx_running_activities_started | started_at | 기간별 통계 |
| user_challenges | idx_user_challenges_user_joined | user_id, joined_at DESC | 참여 목록 조회 |
| user_challenges | idx_user_challenges_user_challenge | user_id, challenge_id (UNIQUE) | 중복 체크 |
| user_challenges | idx_user_challenges_user_completed | user_id, completed_at | 활성 챌린지 |
| challenges | idx_challenges_dates | start_date, end_date | 진행중 챌린지 |
| user_plans | idx_user_plans_user_started | user_id, started_at DESC | 플랜 목록 |
| user_plans | idx_user_plans_user_plan_completed | user_id, plan_id, completed_at | 진행 체크 |
| plan_weeks | idx_plan_weeks_plan_week | plan_id, week_number | 주차별 조회 |
| training_plans | idx_training_plans_goal_difficulty | goal_type, difficulty | 필터링 |

### 인덱스 적용 방법

JPA `@Table` 어노테이션의 `indexes` 속성 사용:

```java
@Entity
@Table(name = "running_activities", indexes = {
    @Index(name = "idx_running_activities_user_started",
           columnList = "user_id, started_at DESC"),
    @Index(name = "idx_running_activities_started",
           columnList = "started_at")
})
public class RunningActivity { ... }
```

### 쿼리 실행 계획 개선 (예상)

| 쿼리 | Before | After |
|------|--------|-------|
| 활동 목록 (10만 건) | Full Table Scan O(n) | Index Scan O(log n) |
| 사용자별 챌린지 | Full Table Scan | Index Seek |
| 진행중 챌린지 | Full Table Scan | Index Range Scan |

### 복합 인덱스 설계 원칙

1. **등호 조건 먼저**: WHERE user_id = ? → user_id가 첫 번째
2. **범위/정렬 나중에**: ORDER BY started_at → started_at이 두 번째
3. **카디널리티 고려**: 선택도가 높은 컬럼 우선

```
좋은 예: (user_id, started_at DESC)
- user_id로 필터링 후 started_at으로 정렬
- 인덱스만으로 정렬 완료 (filesort 불필요)

나쁜 예: (started_at DESC, user_id)
- 전체 started_at 정렬 후 user_id 필터
- 불필요한 데이터 스캔
```

---

## Phase 6: 테스트 커버리지

JaCoCo를 사용하여 테스트 커버리지를 측정하고 관리합니다.

### 현재 커버리지 (총 90개 테스트)

| 레이어 | 커버리지 | 설명 |
|--------|----------|------|
| Controller | **95%** | REST API 통합 테스트 (MockMvc) |
| Service | **82%** | 비즈니스 로직 단위 테스트 (Mockito) |
| Domain | **76%** | 엔티티 메서드 테스트 |
| Event Listeners | **46%** | 이벤트 핸들러 테스트 |
| Config | **26%** | 설정 클래스 (테스트 제외 대상) |
| Scheduler | **6%** | 스케줄러 (통합 테스트 필요) |
| **전체** | **62%** | - |

### 테스트 유형별 분포

| 테스트 유형 | 개수 | 어노테이션 |
|------------|------|-----------|
| Controller 통합 테스트 | 40+ | `@WebMvcTest` |
| Service 단위 테스트 | 35+ | `@ExtendWith(MockitoExtension)` |
| Security 테스트 | 10+ | JWT 인증/인가 검증 |

### 커버리지 측정 방법

```bash
# 테스트 실행 + JaCoCo 리포트 생성
./gradlew test jacocoTestReport

# 리포트 확인
open build/reports/jacoco/test/html/index.html
```

### 커버리지 분석

**높은 커버리지 (80%+)**
- Controller: API 엔드포인트 전체 테스트 완료
- Service: 핵심 비즈니스 로직 검증

**중간 커버리지 (40-79%)**
- Domain: 엔티티 메서드 테스트 (getter/setter 자동 생성 제외)
- Event Listeners: 이벤트 핸들링 테스트

**낮은 커버리지 (40% 미만)**
- Config: 설정 클래스는 일반적으로 테스트 제외
- Scheduler: 시간 기반 테스트의 어려움 (통합 테스트 권장)

### 테스트 품질 지표

| 항목 | 결과 |
|------|------|
| 전체 테스트 수 | 90개 |
| 통과율 | **100%** |
| 빌드 시간 | ~15초 |
| 핵심 로직 커버리지 | **82%+** |

---

## Phase 7: Docker 이미지 최적화

컨테이너 이미지 크기를 줄이고 빌드 캐싱을 최적화했습니다.

### 적용 기술

| 기술 | 설명 | 효과 |
|------|------|------|
| **Alpine 베이스 이미지** | `eclipse-temurin:17-jre-alpine` | 이미지 크기 47% 감소 |
| **Layered JAR** | Spring Boot 레이어 분리 | 빌드 캐시 효율화 |
| **JVM 컨테이너 최적화** | `UseContainerSupport`, `MaxRAMPercentage` | 메모리 효율화 |
| **.dockerignore 확장** | frontend/, ios/, docs/ 제외 | 빌드 컨텍스트 감소 |

### 이미지 크기 비교

| 항목 | Before | After | 감소율 |
|------|--------|-------|--------|
| 베이스 이미지 (JRE) | 274MB (jammy) | 146MB (alpine) | **-47%** |
| 최종 이미지 | ~350MB | ~220MB | **-37%** |

### Layered JAR 구조

Spring Boot의 Layered JAR을 활용하여 Docker 레이어 캐싱을 최적화합니다.

```dockerfile
# 빌드 스테이지에서 레이어 추출
RUN java -Djarmode=layertools -jar build/libs/*.jar extract --destination extracted

# 런타임 스테이지에서 변경 빈도 낮은 순서대로 복사
COPY --from=build /app/extracted/dependencies/ ./         # 1. 외부 라이브러리 (거의 변경 안 됨)
COPY --from=build /app/extracted/spring-boot-loader/ ./   # 2. 스프링 부트 로더
COPY --from=build /app/extracted/snapshot-dependencies/ ./ # 3. 스냅샷 의존성
COPY --from=build /app/extracted/application/ ./          # 4. 애플리케이션 코드 (자주 변경)
```

### 캐싱 효과

소스 코드만 변경된 경우:

| 단계 | 캐시 상태 |
|------|----------|
| dependencies | **HIT** (재사용) |
| spring-boot-loader | **HIT** (재사용) |
| snapshot-dependencies | **HIT** (재사용) |
| application | **MISS** (재빌드) |

결과: 전체 JAR 복사 대비 **빌드 시간 80% 단축**

### JVM 컨테이너 최적화 옵션

```dockerfile
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport",      # 컨테이너 메모리/CPU 제한 인식
    "-XX:MaxRAMPercentage=75.0",     # 컨테이너 메모리의 75% 사용
    "-XX:+UseG1GC",                  # G1 가비지 컬렉터 (낮은 지연시간)
    "-XX:+UseStringDeduplication",   # 문자열 중복 제거 (메모리 절약)
    "-Djava.security.egd=file:/dev/./urandom",  # 빠른 난수 생성
    "org.springframework.boot.loader.launch.JarLauncher"]
```

### docker-compose.yml 리소스 제한

```yaml
services:
  app:
    deploy:
      resources:
        limits:
          memory: 512M
          cpus: '1.0'

  redis:
    deploy:
      resources:
        limits:
          memory: 128M

  postgres:
    deploy:
      resources:
        limits:
          memory: 512M
```

### 빌드 및 실행

```bash
# 이미지 빌드
docker build -t running-app:latest .

# 이미지 크기 확인
docker images running-app:latest

# docker-compose로 전체 스택 실행
docker-compose up --build
```

---

## 기술 스택

| 기술 | 용도 |
|-----|------|
| Spring Boot 3.3 | 백엔드 프레임워크 |
| Spring Data Redis | 캐시 저장소 |
| Spring Events | 이벤트 기반 아키텍처 |
| Spring Retry | 재시도 로직 |
| Spring Scheduling | 스케줄러 |
| K6 | 부하 테스트 |
| **JOIN FETCH** | **N+1 쿼리 최적화** |
| **@Index** | **데이터베이스 인덱스** |
| **JaCoCo** | **테스트 커버리지** |
| **Docker Multi-stage** | **이미지 최적화** |
| **Alpine Linux** | **경량 베이스 이미지** |
| **Layered JAR** | **빌드 캐시 최적화** |

---

## 참고 자료

- [Spring Events](https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html#context-functionality-events)
- [Spring Cache Abstraction](https://docs.spring.io/spring-framework/reference/integration/cache.html)
- [K6 Documentation](https://k6.io/docs/)
- [Spring Boot Docker Layers](https://docs.spring.io/spring-boot/docs/current/reference/html/container-images.html#container-images.dockerfiles)
- [Docker Multi-stage Builds](https://docs.docker.com/build/building/multi-stage/)
