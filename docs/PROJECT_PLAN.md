# Running App 프로젝트 계획서

> 나이키 러닝 앱 스타일 - 러닝 활동 저장, 챌린지/플랜 추천 앱  
> Spring Boot 백엔드 포트폴리오 프로젝트

---

## 1. 핵심 기능 정의

### 1.1 러닝 활동 관리 ✅
- 러닝 기록 저장 (거리, 시간, 페이스, 날짜)
- GPS 경로 데이터 저장 (JSON)
- 활동별 통계 (평균 페이스, 칼로리 소모 등)
- 활동 수정/삭제
- 월별/연도별 요약 통계
- **레벨 업**: 누적 거리 기반 자동 레벨 산정 (Lv1~10)

### 1.2 챌린지 ✅
- 기간별 챌린지 (50km, 100km, 150km / 8회, 12회, 16회)
- 사용자 챌린지 참여/진행률 조회
- 챌린지 완료 여부
- **추천 챌린지**: 레벨 기반 (recommendedMinLevel 필드)

### 1.3 트레이닝 플랜 ✅
- 목표 기반 플랜 추천 (5K, 10K, 하프마라톤)
- **난이도별 플랜**: BEGINNER, INTERMEDIATE, ADVANCED (총 9개 시드)
- 주차별 러닝 스케줄
- **플랜 진행률 추적**: 주차별 목표(거리+횟수) 달성 시 자동 주차 진행
- 사용자 레벨에 맞는 플랜 제안

### 1.4 사용자 관리 ✅
- 회원가입/로그인 (JWT)
- **프로필**: 닉네임, 체중, 신장 수정 API
- 개인 통계 (활동 기반 level, totalDistance)

---

## 2. 기술 스택

### Backend
| 구분 | 기술 | 버전/설명 |
|------|------|------|
| Framework | Spring Boot | 3.3.5 |
| Language | Java | 17 |
| Security | Spring Security + JWT | jjwt 0.12.6 |
| ORM | Spring Data JPA | Hibernate 6.5 |
| DB | H2 (개발) / PostgreSQL | 인메모리/프로덕션 |
| API Docs | SpringDoc OpenAPI | Swagger UI |
| Validation | Jakarta Validation | @Valid, @NotBlank 등 |
| Build | Gradle (Kotlin DSL) | 8.11.1 |
| Testing | JUnit 5, MockMvc, Mockito | Spring Boot Test |

### Frontend (선택)
- React / Next.js (웹)
- React Native / Flutter (모바일 - 추후 고려)

---

## 3. 아키텍처

### 3.1 레이어드 아키텍처
```
controller (REST API)
    ↓
service (비즈니스 로직)
    ↓
repository (데이터 접근)
    ↓
database
```

### 3.2 패키지 구조
```
com.runningapp
├── config/          # Security, JPA, Swagger, 시드 데이터 (ChallengeDataLoader, PlanDataLoader)
├── controller/      # REST 컨트롤러 (Auth, Activity, Challenge, TrainingPlan)
├── service/         # 비즈니스 로직
├── repository/      # JPA Repository
├── domain/          # Entity (User, RunningActivity, Challenge, UserChallenge, TrainingPlan, PlanWeek, UserPlan)
├── dto/             # Request/Response DTO
├── exception/       # GlobalExceptionHandler, BadRequestException, NotFoundException
├── security/        # JwtAuthenticationFilter, UserIdArgumentResolver
└── util/            # JwtUtil, LevelCalculator
```

---

## 4. DB 설계 (핵심 엔티티)

### User
- id, email, password, nickname
- weight, height (선택)
- level (1~10, totalDistance 기반), total_distance
- created_at, updated_at

### RunningActivity
- id, user_id (FK)
- distance, duration, average_pace, calories
- route (JSON - 좌표 배열), started_at, memo
- created_at

### Challenge
- id, name, description
- target_distance, target_count
- start_date, end_date, type (DISTANCE/COUNT)
- recommended_min_level (레벨 기반 추천용)
- created_at

### UserChallenge
- id, user_id, challenge_id
- current_distance, current_count
- completed_at, joined_at

### TrainingPlan
- id, name, description
- goal_type (FIVE_K, TEN_K, HALF_MARATHON)
- difficulty (BEGINNER, INTERMEDIATE, ADVANCED)
- total_weeks, total_runs
- created_at

### PlanWeek
- id, plan_id, week_number
- target_distance, target_runs, description

### UserPlan
- id, user_id, plan_id
- started_at, current_week
- completed_at (null이면 진행중)

---

## 5. API 설계 (최종)

### 인증
| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | /api/auth/signup | 회원가입 |
| POST | /api/auth/login | 로그인 (JWT 발급) |
| GET | /api/auth/me | 내 정보 조회 |
| PATCH | /api/auth/me | 프로필 수정 (닉네임, 체중, 신장) |

### 러닝 활동
| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | /api/activities | 활동 저장 |
| GET | /api/activities | 내 활동 목록 (페이징) |
| GET | /api/activities/{id} | 활동 상세 |
| PUT | /api/activities/{id} | 활동 수정 |
| DELETE | /api/activities/{id} | 활동 삭제 |
| GET | /api/activities/stats | 통계 (year, month) |

### 챌린지
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /api/challenges | 진행중인 챌린지 목록 |
| GET | /api/challenges/recommended | 레벨 기반 추천 챌린지 |
| POST | /api/challenges/{id}/join | 챌린지 참여 |
| GET | /api/challenges/my | 내 참여 챌린지 |
| GET | /api/challenges/{id}/progress | 챌린지 진행률 |

### 플랜
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /api/plans | 플랜 목록 (goalType, difficulty 필터) |
| GET | /api/plans/recommended | 목표/레벨 기반 추천 |
| POST | /api/plans/{id}/start | 플랜 시작 |
| GET | /api/plans/my | 내 진행 플랜 |
| GET | /api/plans/{id}/schedule | 주차별 스케줄 |

---

## 6. 개발 단계 (Phase)

### Phase 1: 기초 ✅
- [x] 프로젝트 셋업 (Spring Boot, DB, JPA)
- [x] User 엔티티, 회원가입/로그인
- [x] JWT 인증
- [x] RunningActivity CRUD
- [x] 기본 통계 API

### Phase 2: 챌린지 ✅
- [x] Challenge, UserChallenge 엔티티
- [x] 챌린지 시드 데이터 (6종)
- [x] 챌린지 참여 API
- [x] 활동 저장 시 챌린지 진행률 업데이트
- [x] 레벨 기반 추천 챌린지

### Phase 3: 트레이닝 플랜 ✅
- [x] TrainingPlan, PlanWeek, UserPlan 엔티티
- [x] 플랜 시드 데이터 (9종: 초/중/고급 × 5K/10K/하프)
- [x] 플랜 추천 로직 (레벨 기반)
- [x] 주차별 진행 로직 (목표 달성 시 자동 주차 진행)
- [x] 주차별 스케줄 API

### Phase 4: 마무리 ✅
- [x] 예외 처리 통합
- [x] API 문서화 (Swagger)
- [x] 테스트 코드
- [x] Docker 배포 설정 (Dockerfile, docker-compose)
- [x] CI/CD (GitHub Actions)

### 추가 구현 ✅
- [x] 레벨 업 로직 (LevelCalculator)
- [x] 프로필 수정 API (PATCH /api/auth/me)

---

## 7. 포트폴리오 포인트

| 항목 | 적용 방법 |
|------|----------|
| **아키텍처** | 레이어드 + DTO 분리, 관심사 분리 |
| **보안** | Spring Security, JWT, BCrypt 비밀번호 |
| **RESTful** | 적절한 HTTP 메서드, 상태 코드, URI 설계 |
| **예외 처리** | @RestControllerAdvice, 커스텀 예외 |
| **문서화** | Swagger/OpenAPI, 상세 주석 |
| **테스트** | MockMvc 통합 테스트 (Auth, Activity, Challenge, Plan) |
| **배포** | Docker, GitHub Actions CI |

---

## 8. 참고 앱

- Nike Run Club: 러닝 기록, 챌린지, 가이드 런
- Strava: 활동, 챌린지, 클럽
- Runkeeper: 플랜, 통계
