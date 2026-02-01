# Running App

나이키 러닝 앱 스타일 - 러닝 활동 저장, 챌린지/플랜 추천 백엔드 API

## 문서

| 문서 | 설명 |
|------|------|
| [docs/USAGE_GUIDE.md](docs/USAGE_GUIDE.md) | **사용 방법** - 실행, API 테스트, 예시 |
| [docs/PROJECT_OVERVIEW.md](docs/PROJECT_OVERVIEW.md) | **상세 설명서** - 기술 스택, 도메인, 아키텍처 |
| [docs/PROJECT_PLAN.md](docs/PROJECT_PLAN.md) | **프로젝트 계획** - 기능, API 설계, Phase |

## 기술 스택

- Java 17, Spring Boot 3.3
- Spring Security + JWT
- Spring Data JPA
- H2 (개발) / PostgreSQL (프로덕션)
- SpringDoc OpenAPI (Swagger)

## 실행 방법

```bash
# 빌드
./gradlew build

# 실행 (H2 인메모리 DB)
./gradlew bootRun

# Docker (PostgreSQL)
docker compose up -d
```

## API 문서

- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 콘솔: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:runningdb`)

## 주요 API

### 인증
- `POST /api/auth/signup` - 회원가입
- `POST /api/auth/login` - 로그인 (JWT 발급)
- `GET /api/auth/me` - 내 정보 (인증 필요)
- `PATCH /api/auth/me` - 프로필 수정 (닉네임, 체중, 신장)

### 러닝 활동
- `POST /api/activities` - 활동 저장 (인증 필요)
- `GET /api/activities` - 내 활동 목록 (페이징)
- `GET /api/activities/{id}` - 활동 상세
- `PUT /api/activities/{id}` - 활동 수정
- `DELETE /api/activities/{id}` - 활동 삭제
- `GET /api/activities/stats` - 통계 (year, month 쿼리 파라미터)

### 챌린지 (Phase 2)
- `GET /api/challenges` - 진행중인 챌린지 목록 (인증 불필요)
- `GET /api/challenges/recommended` - 레벨 기반 추천 챌린지 (인증 필요)
- `POST /api/challenges/{id}/join` - 챌린지 참여
- `GET /api/challenges/my` - 내 참여 챌린지 목록
- `GET /api/challenges/{id}/progress` - 챌린지 진행률 조회

### 트레이닝 플랜 (Phase 3)
- `GET /api/plans` - 플랜 목록 (goalType, difficulty 필터, 인증 불필요)
- `GET /api/plans/recommended` - 레벨 기반 추천 플랜 (인증 필요)
- `POST /api/plans/{id}/start` - 플랜 시작
- `GET /api/plans/my` - 내 진행 플랜 목록
- `GET /api/plans/{id}/schedule` - 주차별 스케줄 (인증 불필요)

## 프로젝트 구조

```
com.runningapp
├── config/          # Security, JPA, Swagger, 시드 데이터
├── controller/      # REST 컨트롤러
├── service/         # 비즈니스 로직
├── repository/      # JPA Repository
├── domain/          # Entity (User, RunningActivity, Challenge, UserChallenge, TrainingPlan, PlanWeek, UserPlan)
├── dto/             # Request/Response DTO
├── exception/       # 예외 처리
├── security/        # JWT 필터, 인증
└── util/            # JWT, LevelCalculator
```

## 주요 기능

- **레벨 시스템**: 누적 거리(km) 기반 Lv1~10
- **챌린지**: 거리/횟수 목표, 레벨별 추천 (6종 시드)
- **트레이닝 플랜**: 5K/10K/하프마라톤 × 초/중/고급 (9종 시드)
- **자동 진행**: 활동 저장 시 챌린지·플랜 진행률 자동 반영
