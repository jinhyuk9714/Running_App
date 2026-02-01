# Running App - 사용 방법 가이드

이 문서는 Running App 백엔드 API를 실행하고 사용하는 방법을 단계별로 설명합니다.

---

## 목차

1. [사전 요구사항](#1-사전-요구사항)
2. [프로젝트 실행](#2-프로젝트-실행)
3. [API 테스트 방법](#3-api-테스트-방법)
4. [주요 API 사용 예시](#4-주요-api-사용-예시)
5. [환경 설정](#5-환경-설정)
6. [문제 해결](#6-문제-해결)

---

## 1. 사전 요구사항

| 항목 | 버전 | 비고 |
|------|------|------|
| Java | 17 이상 | `java -version`으로 확인 |
| Gradle | (Wrapper 사용 시 불필요) | 프로젝트에 `gradlew` 포함 |

### Java 설치 확인

```bash
java -version
# openjdk version "17.x.x" 또는 그 이상
```

---

## 2. 프로젝트 실행

### 2.1 저장소 클론

```bash
git clone https://github.com/jinhyuk9714/Running_App.git
cd Running_App
```

### 2.2 빌드

```bash
./gradlew build
```

- Windows: `gradlew.bat build`
- 테스트 포함 빌드 (통과해야 build 성공)

### 2.3 실행

```bash
./gradlew bootRun
```

- 기본 포트: **8080**
- 로그에 `Started RunningAppApplication` 출력 시 정상 기동

### 2.4 Docker로 실행

```bash
# 빌드 및 실행 (PostgreSQL 포함)
docker compose up -d

# 로그 확인
docker compose logs -f app

# 종료
docker compose down
```

- 앱: http://localhost:8080
- PostgreSQL: localhost:5432 (DB: runningdb, User: running, Password: running123)
- `docker profile` 사용 시 PostgreSQL 연결

### 2.5 접속 확인

| URL | 설명 |
|-----|------|
| http://localhost:8080/swagger-ui.html | Swagger API 문서 |
| http://localhost:8080/api-docs | OpenAPI JSON |
| http://localhost:8080/h2-console | H2 DB 콘솔 (개발용) |

---

## 3. API 테스트 방법

### 방법 A: Swagger UI (권장)

1. 브라우저에서 http://localhost:8080/swagger-ui.html 접속
2. **회원가입** 또는 **로그인** 실행
3. 응답의 `accessToken` 복사
4. 상단 **Authorize** 클릭 → `Bearer {복사한 토큰}` 입력
5. 이후 API 호출 시 자동으로 Authorization 헤더 포함

### 방법 B: curl

```bash
# 1. 회원가입
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123","nickname":"러너"}'

# 응답에서 accessToken 확인 후:
# 2. 내 정보 조회
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer {accessToken}"
```

### 방법 C: Postman / Insomnia

- Base URL: `http://localhost:8080`
- 인증: Bearer Token (로그인 후 accessToken 사용)

---

## 4. 주요 API 사용 예시

### 4.1 인증

#### 회원가입

```http
POST /api/auth/signup
Content-Type: application/json

{
  "email": "runner@example.com",
  "password": "password123",
  "nickname": "러너"
}
```

**응답 예시:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "user": {
    "id": 1,
    "email": "runner@example.com",
    "nickname": "러너",
    "level": 1,
    "totalDistance": 0.0
  }
}
```

#### 로그인

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "runner@example.com",
  "password": "password123"
}
```

#### 프로필 수정 (인증 필요)

```http
PATCH /api/auth/me
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "nickname": "수정된닉네임",
  "weight": 70.0,
  "height": 175.0
}
```

- 전송한 필드만 수정됨 (부분 수정 지원)

---

### 4.2 러닝 활동

#### 활동 저장

```http
POST /api/activities
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "distance": 5.2,
  "duration": 1800,
  "averagePace": 346,
  "calories": 320,
  "startedAt": "2025-02-01T07:00:00",
  "memo": "아침 러닝"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| distance | number | O | 거리(km) |
| duration | integer | O | 시간(초) |
| averagePace | integer | O | 평균 페이스(초/km) |
| calories | integer | O | 칼로리 |
| startedAt | string | O | 시작 시각 (ISO 8601) |
| memo | string | X | 메모 |
| route | array | X | GPS 좌표 배열 |

#### 활동 목록 조회 (페이징)

```http
GET /api/activities?page=0&size=10
Authorization: Bearer {accessToken}
```

#### 통계 조회

```http
# 월별
GET /api/activities/stats?year=2025&month=2

# 연도별
GET /api/activities/stats?year=2025

# 전체 누적
GET /api/activities/stats
```

---

### 4.3 챌린지

#### 진행중인 챌린지 목록 (인증 불필요)

```http
GET /api/challenges
```

#### 추천 챌린지 (인증 필요)

```http
GET /api/challenges/recommended
Authorization: Bearer {accessToken}
```

- 사용자 레벨에 맞는 챌린지만 반환
- 아직 참여하지 않은 챌린지만 포함

#### 챌린지 참여

```http
POST /api/challenges/{challengeId}/join
Authorization: Bearer {accessToken}
```

#### 내 챌린지 목록

```http
GET /api/challenges/my
Authorization: Bearer {accessToken}
```

---

### 4.4 트레이닝 플랜

#### 플랜 목록 (필터)

```http
# 전체
GET /api/plans

# 목표별
GET /api/plans?goalType=FIVE_K

# 난이도별
GET /api/plans?difficulty=BEGINNER

# 조합
GET /api/plans?goalType=TEN_K&difficulty=INTERMEDIATE
```

- **goalType**: FIVE_K, TEN_K, HALF_MARATHON
- **difficulty**: BEGINNER, INTERMEDIATE, ADVANCED

#### 추천 플랜

```http
GET /api/plans/recommended
Authorization: Bearer {accessToken}

# 목표 지정
GET /api/plans/recommended?goalType=TEN_K
```

#### 플랜 시작

```http
POST /api/plans/{planId}/start
Authorization: Bearer {accessToken}
```

- 이미 진행중인 플랜은 중복 시작 불가

#### 주차별 스케줄 (인증 불필요)

```http
GET /api/plans/{planId}/schedule
```

---

## 5. 환경 설정

### 5.1 application.yml 주요 항목

| 키 | 기본값 | 설명 |
|----|--------|------|
| spring.datasource.url | jdbc:h2:mem:runningdb... | H2 DB URL |
| jwt.secret | (내장값) | JWT 서명 키 (프로덕션 시 환경변수 권장) |
| jwt.expiration-ms | 86400000 | 토큰 만료(ms), 24시간 |

### 5.2 프로덕션(PostgreSQL) 전환

`application.yml` 또는 `application-prod.yml`에서:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/runningdb
    username: your_user
    password: your_password
    driver-class-name: org.postgresql.Driver
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate  # create-drop 사용 금지
```

### 5.3 환경변수로 설정 주입

```bash
export JWT_SECRET=your-production-secret-key-min-256-bits
./gradlew bootRun --args='--jwt.secret=${JWT_SECRET}'
```

---

## 6. CI/CD

### GitHub Actions

- **트리거**: `main`, `develop` 브랜치 push 또는 PR 시
- **작업**:
  1. JDK 17 설정, Gradle 캐시
  2. `./gradlew build` 실행 (테스트 포함)
  3. 빌드 산출물 업로드
  4. Docker 이미지 빌드 검증

- 워크플로우: `.github/workflows/ci.yml`

---

## 7. 문제 해결

### Docker 빌드 실패

- `gradle-wrapper.jar` 누락 시: `./gradlew wrapper` 실행 후 재시도
- Gradle 의존성 다운로드 실패 시: 네트워크 확인, `--no-daemon` 옵션 사용

### 포트 8080 사용 중

```bash
# 다른 포트로 실행
./gradlew bootRun --args='--server.port=9090'
```

### H2 콘솔 접속

- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:runningdb`
- User: `sa`
- Password: (비움)

### JWT 토큰 만료

- 로그인 API를 다시 호출하여 새 토큰 발급
- `jwt.expiration-ms` 값으로 만료 시간 조정 가능

### 테스트 프로파일

- `@Profile("!test")`로 시드 데이터 로더는 테스트 시 실행되지 않음
- 테스트용 DB는 H2 인메모리 사용

### 빌드 실패 시

```bash
./gradlew clean build --no-daemon
```

---

## 문서 목록

| 문서 | 설명 |
|------|------|
| [README.md](../README.md) | 프로젝트 요약, 빠른 시작 |
| [PROJECT_PLAN.md](./PROJECT_PLAN.md) | 개발 계획, Phase 정리 |
| [PROJECT_OVERVIEW.md](./PROJECT_OVERVIEW.md) | 상세 기술 설명, 도메인 모델 |
| [USAGE_GUIDE.md](./USAGE_GUIDE.md) | 사용 방법 (본 문서) |
