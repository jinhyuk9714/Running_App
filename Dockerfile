# =============================================================================
# Multi-stage Optimized Dockerfile for Spring Boot
# 최적화: Alpine 이미지, Layered JAR, JVM 컨테이너 설정
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1: Build
# -----------------------------------------------------------------------------
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Gradle wrapper 복사 (캐시 레이어)
COPY gradlew .
COPY gradle gradle
RUN chmod +x gradlew

# 빌드 설정 파일 복사 (의존성 캐시 레이어)
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle.properties ./

# 의존성 다운로드 (소스 변경 시에도 캐시 유지)
RUN ./gradlew dependencies --no-daemon || true

# 소스 복사 및 빌드
COPY src/main src/main
RUN ./gradlew bootJar --no-daemon -x test

# Layered JAR 추출 (Docker 레이어 캐싱 최적화)
RUN java -Djarmode=layertools -jar build/libs/*.jar extract --destination extracted

# -----------------------------------------------------------------------------
# Stage 2: Runtime
# -----------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# 보안: non-root 사용자 생성
RUN addgroup -S spring && adduser -S spring -G spring

# 타임존 설정 (Asia/Seoul)
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone && \
    apk del tzdata

# Layered JAR 복사 (변경 빈도 낮은 순서대로 - 캐시 최적화)
COPY --from=build /app/extracted/dependencies/ ./
COPY --from=build /app/extracted/spring-boot-loader/ ./
COPY --from=build /app/extracted/snapshot-dependencies/ ./
COPY --from=build /app/extracted/application/ ./

# 사용자 전환
USER spring:spring

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

# JVM 컨테이너 최적화 옵션
# - UseContainerSupport: 컨테이너 메모리 제한 인식
# - MaxRAMPercentage: 컨테이너 메모리의 75% 사용
# - +UseG1GC: G1 가비지 컬렉터 (낮은 지연시간)
# - +UseStringDeduplication: 문자열 중복 제거 (메모리 절약)
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-XX:+UseStringDeduplication", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "org.springframework.boot.loader.launch.JarLauncher"]
