# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Full-stack Nike Run Club-style running app with:
- **Backend**: Spring Boot 3.3 REST API (Java 17, Gradle, JWT auth)
- **Frontend**: React 18 + TypeScript + Vite + Tailwind CSS
- **iOS**: SwiftUI app with GPS tracking and HealthKit integration
- **Database**: H2 (dev) / PostgreSQL (prod)
- **Deployment**: NCP with Nginx + Let's Encrypt HTTPS

## Build & Run Commands

### Backend
```bash
./gradlew bootRun                              # Start API on :8080 (H2 in-memory)
./gradlew test                                 # Run all tests
./gradlew test --tests AuthControllerTest      # Run specific test class
./gradlew build                                # Build JAR (output: build/libs/)
```

### Frontend
```bash
cd frontend
npm install
npm run dev      # Dev server on :3000 (proxies /api to :8080)
npm run build    # Production build
```

### iOS
```bash
open ios/RunningApp/RunningApp.xcodeproj       # Open in Xcode, then Cmd+R to run
```

### Docker
```bash
docker-compose up --build    # Full stack with PostgreSQL
```

## Architecture

### Backend (`src/main/java/com/runningapp/`)

```
config/       - SecurityConfig (JWT filter), OpenApiConfig, DataLoaders (seed data)
controller/   - REST endpoints: Auth, RunningActivity, Challenge, TrainingPlan
service/      - Business logic layer
repository/   - Spring Data JPA repositories
domain/       - JPA entities: User, RunningActivity, Challenge, UserChallenge, TrainingPlan, PlanWeek, UserPlan
dto/          - Request/Response objects grouped by feature (auth/, activity/, challenge/, plan/)
security/     - JwtAuthenticationFilter, AuthenticationPrincipal annotation, UserIdArgumentResolver
exception/    - GlobalExceptionHandler with BadRequestException, NotFoundException
```

**Key Patterns**:
- `@AuthenticationPrincipal Long userId` injects authenticated user ID in controllers
- Level system (1-10) based on cumulative distance, auto-calculated on activity changes
- Challenge/Plan progress auto-updates when activities are saved

### Frontend (`frontend/src/`)

```
pages/        - Login, Signup, Dashboard, ActivityList, ActivityDetail, Challenges, Plans, Profile
components/   - Reusable UI (Layout with nav)
api/          - API client wrapper
types.ts      - TypeScript interfaces
```

### iOS (`ios/RunningApp/RunningApp/`)

```
RunTrackingView.swift     - Real-time GPS/HealthKit recording during runs
LocationManager.swift     - Core Location wrapper
HealthKitManager.swift    - Heart rate, cadence, step count
APIClient.swift           - REST API communication
```

## API Structure

Base: `http://localhost:8080` (dev) or `https://jinhyuk-portfolio1.shop` (prod)

- `POST /api/auth/signup|login` - Authentication (returns JWT)
- `GET|PATCH /api/auth/me` - Profile
- `GET|POST|PUT|DELETE /api/activities` - CRUD with pagination, GPS routes as JSON
- `GET /api/activities/summary|stats` - Weekly/monthly summaries
- `GET /api/challenges`, `POST /api/challenges/{id}/join`, `GET /api/challenges/my` - Challenges
- `GET /api/plans`, `POST /api/plans/{id}/start`, `GET /api/plans/my` - Training plans

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Configuration

- `application.yml` - Default (H2, dev mode)
- `application-docker.yml` - PostgreSQL for Docker
- `application-prod.yml` - Production PostgreSQL

JWT secret and expiration configured in application.yml. Production uses env vars: `SPRING_DATASOURCE_URL`, `JWT_SECRET`, etc.

## CI/CD

GitHub Actions (`.github/workflows/ci.yml`):
1. Build: JDK 17, Gradle build with tests
2. Docker: Multi-stage image build
3. Deploy (main only): SCP JAR to NCP server, restart systemd service

Required secrets: `DEPLOY_SSH_KEY`, `DEPLOY_HOST`, `DEPLOY_USER`

## Seed Data

On startup, DataLoaders populate:
- 6 challenges (distance/count based with level recommendations)
- 9 training plans (5K/10K/Half Marathon × Beginner/Intermediate/Advanced)
