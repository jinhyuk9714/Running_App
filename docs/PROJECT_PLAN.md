# Running App 프로젝트 계획서

> 나이키 러닝 앱 스타일 - 러닝 활동 저장, 챌린지/플랜 추천 앱  
> Spring Boot 백엔드 포트폴리오 프로젝트

---

## 1. 핵심 기능 정의

### 1.1 러닝 활동 관리
- 러닝 기록 저장 (거리, 시간, 페이스, 날짜)
- GPS 경로 데이터 저장
- 활동별 통계 (평균 페이스, 칼로리 소모 등)
- 활동 수정/삭제
- 월별/연도별 요약 통계

### 1.2 챌린지
- 기간별 챌린지 (예: 이번 달 100km 달리기)
- 사용자 챌린지 참여/진행률 조회
- 챌린지 완료 여부
- 추천 챌린지 (레벨/목표 기반)

### 1.3 트레이닝 플랜
- 목표 기반 플랜 추천 (5K, 10K, 하프마라톤 등)
- 주차별 러닝 스케줄
- 플랜 진행률 추적
- 사용자 레벨/역량에 맞는 플랜 제안

### 1.4 사용자 관리
- 회원가입/로그인 (JWT)
- 프로필 (체중, 목표, 레벨 등)
- 개인 통계 대시보드

---

## 2. 기술 스택

### Backend
| 구분 | 기술 | 이유 |
|------|------|------|
| Framework | Spring Boot 3.x | Java 17+, 최신 Spring 생태계 |
| Security | Spring Security + JWT | 인증/인가, 포트폴리오에서 자주 묻는 부분 |
| ORM | Spring Data JPA | 생산성, 다양한 쿼리 메서드 |
| DB | PostgreSQL | JSON 타입 지원(GPS 데이터), 무료 |
| API Docs | SpringDoc OpenAPI | Swagger UI, API 문서화 |
| Validation | Jakarta Validation | 요청 검증 |
| Testing | JUnit 5, Mockito | 단위/통합 테스트 |

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

### 3.2 패키지 구조 (제안)
```
com.runningapp
├── config/          # Security, JPA 설정
├── controller/      # REST 컨트롤러
├── service/         # 비즈니스 로직
├── repository/      # JPA Repository
├── domain/          # Entity
├── dto/             # Request/Response DTO
├── exception/       # 예외 처리 (GlobalExceptionHandler)
└── util/            # 공통 유틸
```

---

## 4. DB 설계 (핵심 엔티티)

### User
- id, email, password, nickname
- weight, height (선택)
- level, total_distance
- created_at, updated_at

### RunningActivity
- id, user_id (FK)
- distance, duration, average_pace
- calories, route (JSON - 좌표 배열)
- started_at, memo
- created_at

### Challenge
- id, name, description
- target_distance, target_count (예: 12회 달리기)
- start_date, end_date
- type (DISTANCE / COUNT / CUSTOM)
- created_at

### UserChallenge (참여)
- id, user_id, challenge_id
- current_distance, current_count
- completed_at (null이면 미완료)
- joined_at

### TrainingPlan
- id, name, description
- goal_type (5K, 10K, HALF_MARATHON)
- difficulty (BEGINNER, INTERMEDIATE, ADVANCED)
- total_weeks, total_runs
- created_at

### PlanWeek (플랜별 주차)
- id, plan_id, week_number
- target_distance, target_runs
- description

### UserPlan (사용자 플랜 진행)
- id, user_id, plan_id
- started_at, current_week
- completed_at (null이면 진행중)

---

## 5. API 설계 (핵심)

### 인증
| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | /api/auth/signup | 회원가입 |
| POST | /api/auth/login | 로그인 (JWT 발급) |
| GET | /api/auth/me | 내 정보 조회 |

### 러닝 활동
| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | /api/activities | 활동 저장 |
| GET | /api/activities | 내 활동 목록 (페이징, 필터) |
| GET | /api/activities/{id} | 활동 상세 |
| PUT | /api/activities/{id} | 활동 수정 |
| DELETE | /api/activities/{id} | 활동 삭제 |
| GET | /api/activities/stats | 통계 (월별, 연도별) |

### 챌린지
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /api/challenges | 진행중인 챌린지 목록 |
| GET | /api/challenges/recommended | 추천 챌린지 |
| POST | /api/challenges/{id}/join | 챌린지 참여 |
| GET | /api/challenges/my | 내 참여 챌린지 |
| GET | /api/challenges/{id}/progress | 챌린지 진행률 |

### 플랜
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /api/plans | 플랜 목록 |
| GET | /api/plans/recommended | 목표/레벨 기반 추천 |
| POST | /api/plans/{id}/start | 플랜 시작 |
| GET | /api/plans/my | 내 진행 플랜 |
| GET | /api/plans/{id}/schedule | 주차별 스케줄 |

---

## 6. 개발 단계 (Phase)

### Phase 1: 기초 (1~2주)
- [ ] 프로젝트 셋업 (Spring Boot, DB, JPA)
- [ ] User 엔티티, 회원가입/로그인
- [ ] JWT 인증
- [ ] RunningActivity CRUD
- [ ] 기본 통계 API

### Phase 2: 챌린지 (1주)
- [ ] Challenge, UserChallenge 엔티티
- [ ] 챌린지 CRUD (관리자용 또는 시드 데이터)
- [ ] 챌린지 참여 API
- [ ] 활동 저장 시 챌린지 진행률 업데이트
- [ ] 추천 챌린지 로직

### Phase 3: 트레이닝 플랜 (1~2주)
- [ ] TrainingPlan, PlanWeek 엔티티
- [ ] 플랜 시드 데이터 (5K, 10K 등)
- [ ] 플랜 추천 로직 (레벨/목표 기반)
- [ ] UserPlan, 진행률 추적
- [ ] 주차별 스케줄 API

### Phase 4: 마무리 (1주)
- [ ] 예외 처리 통합
- [ ] API 문서화 (Swagger)
- [ ] 테스트 코드
- [ ] Docker 배포 설정
- [ ] README 정리

---

## 7. 포트폴리오 포인트

| 항목 | 적용 방법 |
|------|----------|
| **아키텍처** | 레이어드 + DTO 분리, 관심사 분리 |
| **보안** | Spring Security, JWT, BCrypt 비밀번호 |
| **RESTful** | 적절한 HTTP 메서드, 상태 코드, URI 설계 |
| **예외 처리** | @ControllerAdvice, 커스텀 예외 |
| **문서화** | Swagger/OpenAPI |
| **테스트** | 서비스 단위 테스트, 컨트롤러 통합 테스트 |
| **배포** | Docker, CI/CD (GitHub Actions) |

---

## 8. 참고/참고할 만한 앱

- Nike Run Club: 러닝 기록, 챌린지, 가이드 런
- Strava: 활동, 챌린지, 클럽
- Runkeeper: 플랜, 통계

---

## 9. 다음 단계

1. 이 계획 검토 후 수정할 부분 정리
2. Phase 1부터 프로젝트 생성 및 구현 시작
3. DB ERD 툴로 상세 설계 (선택)
