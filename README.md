# Running App

나이키 러닝 앱 스타일 - 러닝 활동 저장, 챌린지/플랜 추천 백엔드 API

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

# 실행
./gradlew bootRun
```

## API 문서

- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 콘솔: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:runningdb`)

## 주요 API

### 인증
- `POST /api/auth/signup` - 회원가입
- `POST /api/auth/login` - 로그인 (JWT 발급)
- `GET /api/auth/me` - 내 정보 (인증 필요)

### 러닝 활동
- `POST /api/activities` - 활동 저장 (인증 필요)
- `GET /api/activities` - 내 활동 목록 (페이징)
- `GET /api/activities/{id}` - 활동 상세
- `PUT /api/activities/{id}` - 활동 수정
- `DELETE /api/activities/{id}` - 활동 삭제
- `GET /api/activities/stats` - 통계 (year, month 쿼리 파라미터)

## 프로젝트 구조

```
com.runningapp
├── config/          # Security, JPA, Swagger 설정
├── controller/      # REST 컨트롤러
├── service/         # 비즈니스 로직
├── repository/      # JPA Repository
├── domain/          # Entity (User, RunningActivity)
├── dto/             # Request/Response DTO
├── exception/       # 예외 처리
├── security/        # JWT 필터, 인증
└── util/            # JWT 유틸
```
