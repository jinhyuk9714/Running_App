package com.runningapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 애플리케이션 진입점
 *
 * @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan
 * - Configuration: 이 클래스가 Bean 정의를 포함함
 * - EnableAutoConfiguration: Spring Boot의 자동 설정 활성화 (DB, Security 등)
 * - ComponentScan: com.runningapp 패키지 하위의 @Component들을 스캔하여 Bean 등록
 */
@SpringBootApplication
public class RunningAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(RunningAppApplication.class, args);
    }
}
