# Running App (웹)

백엔드 API **전체 엔드포인트**에 연결한 웹 프론트엔드입니다. 로그인·회원가입, 러닝 기록·요약·통계·삭제, 챌린지·플랜·내 정보를 모두 웹에서 볼 수 있습니다.

## 요구 사항

- Node.js 18+
- 백엔드: `https://jinhyuk-portfolio1.shop` 또는 로컬 `http://localhost:8080`

## 설치 및 실행

```bash
cd frontend
npm install
npm run dev
```

브라우저에서 **http://localhost:3000** 으로 접속합니다.

## API 주소 설정

- **로컬 개발**: `npm run dev` 시 Vite 프록시가 `/api` 요청을 `http://localhost:8080` 으로 넘깁니다. 별도 설정 없이 백엔드만 8080에서 실행하면 됩니다.
- **다른 서버 사용**: 프로젝트 루트에 `.env` 파일을 만들고 다음을 설정합니다.

  ```
  VITE_API_BASE_URL=https://jinhyuk-portfolio1.shop
  ```

## 빌드 (배포용)

```bash
npm run build
```

`dist/` 폴더가 생성됩니다. Nginx 등 웹 서버에서 이 폴더를 정적 파일로 서빙하면 됩니다.

## 기능 (백엔드 엔드포인트 대응)

| 메뉴 / 경로                     | 설명                                            | API                                                                                                                                           |
| ------------------------------- | ----------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- |
| **대시보드** `/`                | 전체 누적·기간별 요약(이번 주/달, 지난달)       | GET /api/activities/summary, GET /api/activities/stats                                                                                        |
| **러닝 기록** `/activities`     | 내 러닝 활동 목록                               | GET /api/activities                                                                                                                           |
| **활동 상세** `/activities/:id` | 상세 + 지도 경로 + 삭제                         | GET /api/activities/:id, DELETE /api/activities/:id                                                                                           |
| **챌린지** `/challenges`        | 진행중·추천·내 챌린지, 참여, 진행률 상세        | GET /api/challenges, GET /api/challenges/recommended, GET /api/challenges/my, POST /api/challenges/:id/join, GET /api/challenges/:id/progress |
| **플랜** `/plans`, `/plans/:id` | 플랜 목록(필터)·추천·내 플랜·시작·주차별 스케줄 | GET /api/plans, GET /api/plans/recommended, GET /api/plans/my, POST /api/plans/:id/start, GET /api/plans/:id/schedule                         |
| **내 정보** `/profile`          | 내 정보 조회·프로필 수정(닉네임·체중·신장)      | GET /api/auth/me, PATCH /api/auth/me                                                                                                          |

- **인증**: POST /api/auth/login, POST /api/auth/signup (로그인·회원가입)
- 지도: OpenStreetMap(Leaflet) 사용
