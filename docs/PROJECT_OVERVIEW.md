# Running App - 상세 프로젝트 설명서

## 개요

Running App은 나이키 러닝 앱(Nike Run Club) 스타일의 러닝 기록 관리 백엔드 API입니다. 사용자가 러닝 활동을 기록하고, 챌린지에 참여하며, 트레이닝 플랜을 따라 목표를 달성할 수 있도록 지원합니다.

### 주요 특징

- **JWT 기반 인증**: stateless API 인증
- **레벨 시스템**: 누적 거리(km)에 따른 1~10 레벨 산정
- **챌린지**: 거리/횟수 목표, 레벨별 추천
- **트레이닝 플랜**: 5K, 10K, 하프마라톤 목표별 초/중/고급 플랜
- **자동 진행 추적**: 활동 저장 시 챌린지·플랜 진행률 자동 반영

---

## 기술 스택 상세

| 구분        | 기술                      | 버전   | 용도                       |
| ----------- | ------------------------- | ------ | -------------------------- |
| 언어        | Java                      | 17     | LTS, Records 등 모던 문법  |
| 프레임워크  | Spring Boot               | 3.3.5  | REST API, DI, 자동 설정    |
| 빌드        | Gradle (Kotlin DSL)       | 8.11.1 | 의존성, 빌드 스크립트      |
| ORM         | Spring Data JPA           | -      | 엔티티 매핑, 쿼리 메서드   |
| DB          | H2 / PostgreSQL           | -      | 개발용 인메모리 / 프로덕션 |
| 보안        | Spring Security           | -      | 인증·인가, JWT 필터        |
| JWT         | jjwt                      | 0.12.6 | 토큰 생성·검증             |
| API 문서    | SpringDoc OpenAPI         | 2.6.0  | Swagger UI                 |
| 유효성 검사 | Jakarta Validation        | -      | @Valid, @NotBlank 등       |
| 테스트      | JUnit 5, MockMvc, Mockito | -      | 통합 테스트                |

---

## 프로젝트 구조

```
Running_App/
├── build.gradle.kts          # Gradle 의존성 및 빌드 설정
├── settings.gradle.kts
├── gradle.properties
├── README.md
├── docs/
│   ├── DEPLOY_NCP.md         # NCP 배포 가이드
│   └── PROJECT_OVERVIEW.md   # 상세 설명서 (본 문서)
└── src/
    ├── main/
    │   ├── java/com/runningapp/
    │   │   ├── RunningAppApplication.java
    │   │   ├── config/           # 설정
    │   │   │   ├── SecurityConfig.java      # Spring Security, JWT
    │   │   │   ├── OpenApiConfig.java       # Swagger 설정
    │   │   │   ├── WebMvcConfig.java        # ArgumentResolver 등록
    │   │   │   ├── ChallengeDataLoader.java # 챌린지 시드 데이터
    │   │   │   └── PlanDataLoader.java      # 트레이닝 플랜 시드 데이터
    │   │   ├── controller/       # REST 컨트롤러
    │   │   │   ├── AuthController.java
    │   │   │   ├── RunningActivityController.java
    │   │   │   ├── ChallengeController.java
    │   │   │   └── TrainingPlanController.java
    │   │   ├── service/          # 비즈니스 로직
    │   │   │   ├── AuthService.java
    │   │   │   ├── RunningActivityService.java
    │   │   │   ├── ChallengeService.java
    │   │   │   └── TrainingPlanService.java
    │   │   ├── repository/       # JPA Repository
    │   │   ├── domain/           # JPA Entity
    │   │   ├── dto/              # Request/Response DTO
    │   │   ├── exception/        # 예외 처리
    │   │   ├── security/         # JWT 필터, 인증 관련
    │   │   └── util/             # 유틸리티
    │   └── resources/
    │       └── application.yml   # 애플리케이션 설정
    └── test/
        └── java/com/runningapp/
            ├── controller/       # 컨트롤러 통합 테스트
            └── util/TestUtils.java
```

---

## 도메인 모델

### User (사용자)

- **id**: Primary Key
- **email**: 로그인 ID (unique)
- **password**: BCrypt 해시
- **nickname**: 표시 이름
- **weight, height**: 선택 입력 (체중 kg, 신장 cm)
- **level**: 1~10 (누적 거리 기반)
- **totalDistance**: 누적 러닝 거리(km)
- **createdAt, updatedAt**: 감사 필드

### RunningActivity (러닝 활동)

- **id, userId**: 활동 식별
- **distance**: 거리(km)
- **duration**: 시간(초)
- **averagePace**: 평균 페이스(초/km)
- **calories**: 소모 칼로리
- **route**: GPS 경로 (JSON 배열)
- **startedAt**: 러닝 시작 시각
- **memo**: 메모
- **createdAt**: 생성 시각

### Challenge (챌린지)

- **id, name, description**: 챌린지 정보
- **targetDistance / targetCount**: 목표(거리 km 또는 횟수)
- **startDate, endDate**: 진행 기간
- **type**: DISTANCE(거리 목표) | COUNT(횟수 목표)
- **recommendedMinLevel**: 추천 최소 레벨 (1~10)

### UserChallenge (챌린지 참여)

- **userId, challengeId**: 참여 정보
- **currentDistance, currentCount**: 현재 진행량
- **completedAt**: 완료 시각 (null이면 진행중)
- **joinedAt**: 참여 시각

### TrainingPlan (트레이닝 플랜)

- **id, name, description**: 플랜 정보
- **goalType**: FIVE_K | TEN_K | HALF_MARATHON
- **difficulty**: BEGINNER | INTERMEDIATE | ADVANCED
- **totalWeeks, totalRuns**: 총 주차, 총 러닝 횟수

### PlanWeek (주차별 스케줄)

- **planId, weekNumber**: 플랜 내 주차
- **targetDistance, targetRuns**: 해당 주 목표
- **description**: 설명

### UserPlan (플랜 진행)

- **userId, planId**: 진행 정보
- **startedAt**: 시작 시각
- **currentWeek**: 현재 주차 (1~)
- **completedAt**: 완료 시각 (null이면 진행중)

---

## 레벨 시스템

`LevelCalculator` 유틸이 누적 거리(km)에 따라 레벨을 산정합니다.

| 레벨 | 필요 누적 거리 |
| ---- | -------------- |
| 1    | 0 km           |
| 2    | 10 km          |
| 3    | 25 km          |
| 4    | 50 km          |
| 5    | 100 km         |
| 6    | 200 km         |
| 7    | 400 km         |
| 8    | 700 km         |
| 9    | 1,000 km       |
| 10   | 1,500 km       |

- 활동 생성·수정·삭제 시 `User.totalDistance`가 변경될 때마다 `updateLevel()` 호출
- 트레이닝 플랜·챌린지 추천 시 사용자 레벨 반영

---

## 시드 데이터

### 챌린지 (6종)

| 이름                 | 유형 | 목표   | 추천 레벨 |
| -------------------- | ---- | ------ | --------- |
| 이번 달 50km 달리기  | 거리 | 50 km  | 1         |
| 이번 달 100km 달리기 | 거리 | 100 km | 4         |
| 이번 달 150km 달리기 | 거리 | 150 km | 7         |
| 이번 달 8회 달리기   | 횟수 | 8 회   | 1         |
| 이번 달 12회 달리기  | 횟수 | 12 회  | 3         |
| 이번 달 16회 달리기  | 횟수 | 16 회  | 6         |

### 트레이닝 플랜 (9종)

| 목표       | 초급 | 중급 | 고급 |
| ---------- | ---- | ---- | ---- |
| 5K         | 8주  | 6주  | 4주  |
| 10K        | 10주 | 8주  | 6주  |
| 하프마라톤 | 12주 | 10주 | 8주  |

---

## 보안

### 인증 흐름

1. `POST /api/auth/login` 또는 `signup` → `accessToken` 수신
2. 이후 요청 시 `Authorization: Bearer {accessToken}` 헤더에 토큰 포함

### 인증 필요 API

- 대부분의 `/api/**` 경로는 인증 필요
- 예외(인증 불필요): `/api/auth/signup`, `/api/auth/login`, `GET /api/challenges`, `GET /api/plans`, `GET /api/plans/{id}/schedule`, Swagger/H2 콘솔

### JWT 설정

- `application.yml`의 `jwt.secret`, `jwt.expiration-ms`로 토큰 설정
- 프로덕션에서는 환경변수로 `jwt.secret` 주입 권장

---

## API 엔드포인트 요약

### 인증

| Method | Endpoint         | 설명         |
| ------ | ---------------- | ------------ |
| POST   | /api/auth/signup | 회원가입     |
| POST   | /api/auth/login  | 로그인       |
| GET    | /api/auth/me     | 내 정보 조회 |
| PATCH  | /api/auth/me     | 프로필 수정  |

### 러닝 활동

| Method | Endpoint              | 설명                  |
| ------ | --------------------- | --------------------- |
| POST   | /api/activities       | 활동 저장             |
| GET    | /api/activities       | 내 활동 목록 (페이징) |
| GET    | /api/activities/{id}  | 활동 상세             |
| PUT    | /api/activities/{id}  | 활동 수정             |
| DELETE | /api/activities/{id}  | 활동 삭제             |
| GET    | /api/activities/stats | 통계 (year, month)    |

### 챌린지

| Method | Endpoint                      | 설명                  |
| ------ | ----------------------------- | --------------------- |
| GET    | /api/challenges               | 진행중인 챌린지 목록  |
| GET    | /api/challenges/recommended   | 레벨 기반 추천 챌린지 |
| POST   | /api/challenges/{id}/join     | 챌린지 참여           |
| GET    | /api/challenges/my            | 내 참여 챌린지        |
| GET    | /api/challenges/{id}/progress | 챌린지 진행률         |

### 트레이닝 플랜

| Method | Endpoint                 | 설명             |
| ------ | ------------------------ | ---------------- |
| GET    | /api/plans               | 플랜 목록 (필터) |
| GET    | /api/plans/recommended   | 추천 플랜        |
| POST   | /api/plans/{id}/start    | 플랜 시작        |
| GET    | /api/plans/my            | 내 진행 플랜     |
| GET    | /api/plans/{id}/schedule | 주차별 스케줄    |

상세 요청/응답 스키마는 Swagger UI에서 확인할 수 있습니다.

---

## 예외 처리

`GlobalExceptionHandler`가 공통 에러 응답 형식으로 처리합니다.

```json
{
  "message": "에러 메시지",
  "errors": { "fieldName": "필드별 에러" },
  "timestamp": "2025-02-01T12:00:00"
}
```

- **400 Bad Request**: `BadRequestException` (중복 이메일, 잘못된 입력 등)
- **404 Not Found**: `NotFoundException` (리소스 없음)
- **400 Validation**: `MethodArgumentNotValidException` (@Valid 실패)

---

## 테스트

- **위치**: `src/test/java/com/runningapp/controller/`
- **종류**: MockMvc 기반 REST API 통합 테스트
- **범위**: AuthController, RunningActivityController, ChallengeController, TrainingPlanController

```bash
./gradlew test
```
