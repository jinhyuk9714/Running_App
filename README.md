# Running App

나이키 런 클럽 스타일 풀스택 러닝 앱

- **Backend**: Spring Boot 3.3 REST API (Java 17, Gradle, JWT 인증)
- **Frontend**: React 18 + TypeScript + Vite + Tailwind CSS
- **iOS**: SwiftUI 앱 (GPS 트래킹, HealthKit 연동)
- **Database**: H2 (개발) / PostgreSQL (프로덕션)
- **Cache**: Redis (응답시간 30% 개선)
- **Deployment**: NCP + Nginx + Let's Encrypt HTTPS

---

## 실서비스 (NCP 배포)

| 구분           | URL                                                   |
| -------------- | ----------------------------------------------------- |
| **Swagger UI** | https://jinhyuk-portfolio1.shop/swagger-ui/index.html |
| **API**        | https://jinhyuk-portfolio1.shop/api/...               |
| **Health**     | https://jinhyuk-portfolio1.shop/actuator/health       |

- 회원가입/로그인 후 Swagger 상단 **Authorize**에서 토큰 입력하면 인증 API 테스트 가능
- `api.jinhyuk-portfolio1.shop` 도메인으로도 동일 접속 가능

### 배포·운영 (포트폴리오 요약)

| 구분              | 내용                                                   |
| ----------------- | ------------------------------------------------------ |
| **인프라**        | NCP Server(VPC), Ubuntu, Public Subnet                 |
| **DB**            | NCP Cloud DB for PostgreSQL (VPC 내부 연동)            |
| **네트워크**      | ACG: SSH(22), HTTP(80), HTTPS(443), 앱(8080), DB(5432) |
| **프로세스 관리** | systemd 서비스 등록 (재부팅 시 자동 기동)              |
| **역방향 프록시** | Nginx (80/443 → 8080), Let's Encrypt HTTPS             |
| **CI/CD**         | GitHub Actions: `main` 푸시 시 JAR 빌드·배포·restart   |
| **설정**          | Spring `prod` 프로파일, 환경 변수로 DB·JWT 주입        |

---

## 성능 최적화

단계적 성능 최적화를 통해 응답시간 30% 개선 및 아키텍처 개선을 달성했습니다.

| Phase | 적용 기술 | 주요 개선 |
|-------|----------|----------|
| Phase 1 | Baseline | 초기 측정 (평균 21.94ms) |
| Phase 2 | Redis Caching | 응답시간 **-30.8%**, 에러율 0% |
| Phase 3 | Async Event-Driven | 서비스 결합도 감소, 확장성 향상 |

### 성능 테스트 결과 (K6, 50 VUs)

| 지표 | Baseline | 최종 | 개선율 |
|-----|----------|------|--------|
| 평균 응답시간 | 21.94ms | 15.67ms | **-28.6%** |
| P95 응답시간 | 93.96ms | 75.36ms | **-19.8%** |
| 에러율 | 59.98% | 0.00% | **-100%** |

### 이벤트 기반 비동기 아키텍처

```
POST /api/activities
    → Save Activity
    → Publish Event ──────────→ Response (~5ms)
           │
           ↓ (Async)
    ┌──────┴──────┐
    │  Listeners  │
    ├─────────────┤
    │ UserLevel   │  @TransactionalEventListener
    │ Challenge   │  @Async, @Retryable
    │ TrainingPlan│  독립적 재시도
    └─────────────┘
```

**이점**: 느슨한 결합, 장애 격리, 확장성 (리스너 추가만으로 기능 확장)

자세한 내용은 [docs/PERFORMANCE.md](docs/PERFORMANCE.md) 참고.

---

## 기술 스택

### Backend
- **Java 17**, Spring Boot 3.3
- **Spring Security** + JWT, **Spring Boot Actuator** (Health)
- **Spring Data JPA** (H2 개발 / PostgreSQL 프로덕션)
- **Spring Data Redis** (캐싱)
- **Spring Events** (이벤트 기반 아키텍처)
- **Spring Retry** (재시도 로직)
- **Spring Scheduling** (스케줄러)
- **SpringDoc OpenAPI** (Swagger UI)
- **Gradle** (Kotlin DSL)

### Frontend
- **React 18** + **TypeScript**
- **Vite** (빌드 도구)
- **Tailwind CSS** (스타일링)
- **React Router** (라우팅)

### iOS
- **SwiftUI** (UI 프레임워크)
- **Core Location** (GPS 트래킹)
- **HealthKit** (심박수, 케이던스, 걸음 수)
- **MapKit** (지도 경로 표시)

### DevOps & Testing
- **Docker** + **Docker Compose**
- **GitHub Actions** (CI/CD)
- **K6** (부하 테스트)
- **JUnit 5** + **MockMvc** (테스트)

---

## 로컬 실행

```bash
# 빌드
./gradlew build

# 실행 (H2 인메모리 DB)
./gradlew bootRun

# Redis 실행 (캐싱 사용 시)
redis-server
```

- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **Health**: http://localhost:8080/actuator/health (인증 없이 상태 확인)
- **H2 콘솔**: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:runningdb`)

### 웹 프론트엔드 (React)

```bash
cd frontend
npm install
npm run dev
```

- **웹 앱**: http://localhost:3000 (로그인·회원가입, 러닝 기록 목록·상세·지도)
- 개발 시 Vite 프록시로 `/api` → `localhost:8080` 자동 연결

자세한 설정·배포는 [frontend/README.md](frontend/README.md) 참고.

### iOS 앱

```bash
open ios/RunningApp/RunningApp.xcodeproj   # Xcode에서 열고 Cmd+R로 실행
```

- [ios/README.md](ios/README.md) 참고

### Docker

```bash
docker-compose up --build    # PostgreSQL과 함께 풀스택 실행
```

### 부하 테스트 (K6)

```bash
# Quick test (1분)
k6 run k6/quick-test.js

# Full load test (3분 30초)
k6 run k6/load-test.js
```

---

## 주요 API

| 구분   | Method | Endpoint                    | 설명                  |
| ------ | ------ | --------------------------- | --------------------- |
| 인증   | POST   | /api/auth/signup            | 회원가입              |
| 인증   | POST   | /api/auth/login             | 로그인 (JWT 발급)     |
| 인증   | GET    | /api/auth/me                | 내 정보               |
| 인증   | PATCH  | /api/auth/me                | 프로필 수정           |
| 활동   | POST   | /api/activities             | 활동 저장             |
| 활동   | GET    | /api/activities             | 내 활동 목록 (페이징) |
| 활동   | GET    | /api/activities/{id}        | 활동 상세             |
| 활동   | PUT    | /api/activities/{id}        | 활동 수정             |
| 활동   | DELETE | /api/activities/{id}        | 활동 삭제             |
| 활동   | GET    | /api/activities/summary     | 주간·월간 요약        |
| 활동   | GET    | /api/activities/stats       | 통계 (year, month)    |
| 챌린지 | GET    | /api/challenges             | 진행중인 챌린지       |
| 챌린지 | GET    | /api/challenges/recommended | 레벨 기반 추천        |
| 챌린지 | POST   | /api/challenges/{id}/join   | 챌린지 참여           |
| 챌린지 | GET    | /api/challenges/my          | 내 참여 챌린지        |
| 플랜   | GET    | /api/plans                  | 플랜 목록 (필터)      |
| 플랜   | GET    | /api/plans/recommended      | 추천 플랜             |
| 플랜   | POST   | /api/plans/{id}/start       | 플랜 시작             |
| 플랜   | GET    | /api/plans/my               | 내 진행 플랜          |
| 플랜   | GET    | /api/plans/{id}/schedule    | 주차별 스케줄         |

---

## 문서

| 문서                                                 | 설명                                            |
| ---------------------------------------------------- | ----------------------------------------------- |
| [docs/PERFORMANCE.md](docs/PERFORMANCE.md)           | 성능 최적화 보고서 (Redis, Async, K6 테스트)    |
| [docs/DEPLOY_NCP.md](docs/DEPLOY_NCP.md)             | NCP 배포 가이드 (VPC, Server, DB, ACG, systemd) |
| [docs/HTTPS_SETUP.md](docs/HTTPS_SETUP.md)           | 도메인 HTTPS (Nginx + Let's Encrypt)            |
| [docs/NEXT_STEPS.md](docs/NEXT_STEPS.md)             | 자동 배포 설정 (GitHub Secrets, 서버 sudo)      |
| [docs/PROJECT_OVERVIEW.md](docs/PROJECT_OVERVIEW.md) | 상세 기술·도메인 설명                           |

---

## 주요 기능

- **레벨 시스템**: 누적 거리(km) 기반 Lv1~10
- **챌린지**: 거리/횟수 목표, 레벨별 추천 (6종 시드)
- **트레이닝 플랜**: 5K/10K/하프마라톤 × 초/중/고급 (9종 시드)
- **자동 진행**: 활동 저장 시 챌린지·플랜 진행률 자동 반영 (비동기 이벤트)
- **스케줄러**: 챌린지 만료 처리, 통계 집계, 캐시 워밍업 자동화

---

## iOS 앱 (나이키 러닝 스타일)

- **시작** → 실시간 GPS·지도 경로 표시 → **완료** 시 서버에 결과 저장
- 저장 항목: 거리, 시간, 평균 페이스, 칼로리, 평균 심박수, 케이던스, GPS 경로(지도 표시)
- **위치**: `ios/` — Xcode에서 새 iOS App 생성 후 `ios/RunningApp/Sources` 소스 참고
- **상세**: [ios/README.md](ios/README.md)

---

## 프로젝트 구조

### Backend (`src/main/java/com/runningapp/`)

```
config/       # SecurityConfig, AsyncConfig, SchedulingConfig, DataLoaders
controller/   # REST 엔드포인트: Auth, RunningActivity, Challenge, TrainingPlan
service/      # 비즈니스 로직
repository/   # Spring Data JPA Repository
domain/       # JPA Entity: User, RunningActivity, Challenge, UserChallenge, TrainingPlan, PlanWeek, UserPlan
dto/          # Request/Response (auth/, activity/, challenge/, plan/)
event/        # 이벤트 클래스 및 리스너 (ActivityCompletedEvent, UserLevelEventListener 등)
scheduler/    # 스케줄러 (ChallengeScheduler, CacheWarmupScheduler 등)
exception/    # GlobalExceptionHandler, BadRequestException, NotFoundException
security/     # JwtAuthenticationFilter, AuthenticationPrincipal, UserIdArgumentResolver
```

### Frontend (`frontend/src/`)

```
pages/        # Login, Signup, Dashboard, ActivityList, ActivityDetail, Challenges, Plans, Profile
components/   # 재사용 UI 컴포넌트 (Layout, Nav 등)
api/          # API 클라이언트 래퍼
types.ts      # TypeScript 인터페이스
```

### iOS (`ios/RunningApp/RunningApp/`)

```
RunTrackingView.swift     # 실시간 GPS/HealthKit 기록 화면
LocationManager.swift     # Core Location 래퍼
HealthKitManager.swift    # 심박수, 케이던스, 걸음 수
APIClient.swift           # REST API 통신
```
