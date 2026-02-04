package com.runningapp.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 캐시 설정
 *
 * 캐시 전략:
 * - activitySummary: 사용자별 주간/월간 요약 (5분 TTL)
 * - activityStats: 통계 데이터 (5분 TTL)
 * - activeChallenges: 진행중인 챌린지 목록 (10분 TTL)
 * - plans: 플랜 목록 (30분 TTL, 자주 안 바뀜)
 */
@Configuration
@EnableCaching
@Profile("!test")  // 테스트 환경에서는 비활성화
public class RedisConfig {

    /**
     * Java 8 날짜/시간 타입 직렬화를 지원하는 GenericJackson2JsonRedisSerializer 생성
     *
     * GenericJackson2JsonRedisSerializer 기본 설정 + JavaTimeModule
     */
    private GenericJackson2JsonRedisSerializer createJsonSerializer() {
        ObjectMapper mapper = new ObjectMapper();

        // Java 8 날짜/시간 타입 지원
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // GenericJackson2JsonRedisSerializer 기본 설정과 동일하게 타입 정보 활성화
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();

        mapper.activateDefaultTyping(
                typeValidator,
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY
        );

        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        GenericJackson2JsonRedisSerializer jsonSerializer = createJsonSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jsonSerializer);
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer jsonSerializer = createJsonSerializer();

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        // 캐시별 TTL 설정
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // 활동 요약: 5분 (자주 조회되지만 활동 추가 시 갱신)
        cacheConfigurations.put("activitySummary", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // 활동 통계: 5분
        cacheConfigurations.put("activityStats", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // 진행중인 챌린지: 10분 (시간 기반으로 자주 안 바뀜)
        cacheConfigurations.put("activeChallenges", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // 추천 챌린지: 5분 (사용자 레벨 기반)
        cacheConfigurations.put("recommendedChallenges", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // 플랜 목록: 30분 (거의 안 바뀜)
        cacheConfigurations.put("plans", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // 추천 플랜: 10분
        cacheConfigurations.put("recommendedPlans", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // 플랜 스케줄: 1시간 (절대 안 바뀜)
        cacheConfigurations.put("planSchedule", defaultConfig.entryTtl(Duration.ofHours(1)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
