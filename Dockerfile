# Multi-stage build (Alpine 대신 멀티플랫폼 지원 이미지 사용 - Apple Silicon 등)
# Stage 1: Build
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Gradle wrapper + 설정 파일 복사
COPY gradlew .
COPY gradle gradle
RUN chmod +x gradlew
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle.properties ./

# 의존성만 먼저 다운로드 (캐시 활용)
RUN ./gradlew dependencies --no-daemon || true

# 소스 복사 및 빌드
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Run
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# 보안: root가 아닌 사용자로 실행
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
