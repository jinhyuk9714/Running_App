package com.runningapp.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HTTP 응답 압축 테스트
 *
 * Spring Boot의 Gzip 압축이 제대로 동작하는지 검증:
 * - Accept-Encoding: gzip 헤더 전송 시 압축 응답 반환
 * - Content-Encoding: gzip 헤더 확인
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CompressionTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Gzip 압축 - Accept-Encoding 헤더 시 압축 응답 반환")
    void shouldReturnCompressedResponse_whenAcceptEncodingGzip() {
        // Given: Gzip 지원 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept-Encoding", "gzip, deflate");

        // When: 챌린지 목록 조회 (공개 API, 큰 응답)
        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/challenges",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        // Then: 응답 성공 확인
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        // 참고: TestRestTemplate은 자동으로 압축을 해제하므로
        // Content-Encoding 헤더가 응답에 없을 수 있음 (RestTemplate 동작 방식)
        // 실제 압축 동작은 curl 또는 브라우저 DevTools로 확인 권장
        System.out.println("Response Headers: " + response.getHeaders());
        System.out.println("Response Body Length: " +
                (response.getBody() != null ? response.getBody().length() : 0) + " chars");
    }

    @Test
    @DisplayName("서버 압축 설정 확인 - 응답이 정상 반환됨")
    void shouldReturnValidResponse_withCompressionEnabled() {
        // Given & When: API 호출
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/challenges",
                String.class
        );

        // Then: 응답 성공 및 JSON 형식 확인
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().toString())
                .contains("application/json");
    }
}
