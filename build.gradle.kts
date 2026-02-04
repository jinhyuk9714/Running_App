// Gradle Kotlin DSL
// - Spring Boot: 3.3.5 (Java 17+)
// - dependency-management: Spring BOM으로 버전 관리
plugins {
    java
    jacoco
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "com.runningapp"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starters (웹, JPA, Security, Validation, Cache)
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // Spring Retry (비동기 이벤트 리스너 재시도)
    implementation("org.springframework.retry:spring-retry")
    implementation("org.springframework:spring-aspects")

    // Database: H2(개발), PostgreSQL(프로덕션)
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")

    // JWT: jjwt 라이브러리 (토큰 생성/검증)
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // Swagger/OpenAPI: /swagger-ui.html
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    // Rate Limiting: Bucket4j (Token Bucket 알고리즘)
    implementation("com.bucket4j:bucket4j-core:8.10.1")

    // Structured Logging: JSON 포맷 (ELK 연동 가능)
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    // Lombok: @Getter, @Builder 등 보일러플레이트 제거
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")

    // Test: JUnit5, MockMvc, Spring Security Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)

    // 테스트 병렬 실행 (CI 환경에서 빌드 시간 단축)
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)

    // 테스트 리포트 설정
    reports {
        junitXml.required.set(true)
        html.required.set(true)
    }

    // 테스트 로깅
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

jacoco {
    toolVersion = "0.8.11"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}
