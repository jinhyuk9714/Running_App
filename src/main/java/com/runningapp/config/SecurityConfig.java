package com.runningapp.config;

import com.runningapp.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정
 *
 * JWT 기반 Stateless 인증:
 * - 세션 대신 JWT 토큰으로 인증
 * - 매 요청마다 JWT 검증 → 인증된 사용자 식별
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // REST API이므로 CSRF 비활성화 (세션 기반이 아닐 때)
                .csrf(AbstractHttpConfigurer::disable)
                // Stateless: 세션 생성 안 함 (JWT 사용)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // URL별 접근 권한 설정 (순서 중요 - 먼저 매칭된 규칙 적용)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/signup", "/api/auth/login").permitAll()  // 인증 불필요
                        .requestMatchers(HttpMethod.GET, "/api/challenges").permitAll()  // 진행중 챌린지 목록 공개
                        .requestMatchers(HttpMethod.GET, "/api/plans").permitAll()  // 플랜 목록 공개
                        .requestMatchers(HttpMethod.GET, "/api/plans/*/schedule").permitAll()  // 주차별 스케줄 공개
                        .requestMatchers("/swagger-ui/**", "/api-docs/**", "/h2-console/**").permitAll()
                        .requestMatchers("/api/**").authenticated()  // 나머지 API는 인증 필수
                        .anyRequest().authenticated()
                )
                // H2 콘솔 iframe 허용
                .headers(headers ->
                        headers.frameOptions(frame -> frame.sameOrigin()))
                // JWT 검증 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();  // 비밀번호 해싱
    }
}
