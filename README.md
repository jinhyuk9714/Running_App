# Running App

나이키 러닝 앱 스타일 - 러닝 활동 저장, 챌린지/플랜 추천 백엔드 API

---

## 실서비스 (NCP 배포)

| 구분           | URL                                            |
| -------------- | ---------------------------------------------- |
| **Swagger UI** | http://49.50.131.57:8080/swagger-ui/index.html |
| **API**        | http://49.50.131.57:8080/api/...               |

- 회원가입/로그인 후 Swagger 상단 **Authorize**에서 토큰 입력하면 인증 API 테스트 가능
- Swagger 경로: 반드시 `/swagger-ui/index.html` 사용 (`/swagger-ui.html` 은 파일 다운로드될 수 있음)

### 배포·운영 (포트폴리오 요약)

| 구분              | 내용                                            |
| ----------------- | ----------------------------------------------- |
| **인프라**        | NCP Server(VPC), Ubuntu, Public Subnet          |
| **DB**            | NCP Cloud DB for PostgreSQL (VPC 내부 연동)     |
| **네트워크**      | ACG로 SSH(22), 앱(8080), DB(5432) 접근 제어     |
| **프로세스 관리** | systemd 서비스 등록 (재부팅 시 자동 기동)       |
| **설정**          | Spring `prod` 프로파일, 환경 변수로 DB·JWT 주입 |

---

## 기술 스택

- **Java 17**, Spring Boot 3.3
- **Spring Security** + JWT
- **Spring Data JPA** (H2 개발 / PostgreSQL 프로덕션)
- **SpringDoc OpenAPI** (Swagger UI)
- **Gradle** (Kotlin DSL)

---

## 로컬 실행

```bash
# 빌드
./gradlew build

# 실행 (H2 인메모리 DB)
./gradlew bootRun
```

- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **Health**: http://localhost:8080/actuator/health (인증 없이 상태 확인)
- **H2 콘솔**: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:runningdb`)

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

| 문서                                                 | 설명                             |
| ---------------------------------------------------- | -------------------------------- |
| [docs/DEPLOY_NCP.md](docs/DEPLOY_NCP.md)             | NCP(네이버 클라우드) 배포 가이드 |
| [docs/NEXT_STEPS.md](docs/NEXT_STEPS.md)             | **다음 단계**: GitHub Secrets + 서버 sudo (자동 배포) |
| [docs/PROJECT_OVERVIEW.md](docs/PROJECT_OVERVIEW.md) | 상세 기술·도메인 설명            |

---

## 주요 기능

- **레벨 시스템**: 누적 거리(km) 기반 Lv1~10
- **챌린지**: 거리/횟수 목표, 레벨별 추천 (6종 시드)
- **트레이닝 플랜**: 5K/10K/하프마라톤 × 초/중/고급 (9종 시드)
- **자동 진행**: 활동 저장 시 챌린지·플랜 진행률 자동 반영

---

## 프로젝트 구조

```
com.runningapp
├── config/       # Security, JPA, Swagger, 시드 데이터
├── controller/   # REST (Auth, Activity, Challenge, TrainingPlan)
├── service/      # 비즈니스 로직
├── repository/   # JPA Repository
├── domain/       # Entity
├── dto/          # Request/Response
├── exception/    # GlobalExceptionHandler
├── security/     # JWT 필터
└── util/         # JWT, LevelCalculator
```
