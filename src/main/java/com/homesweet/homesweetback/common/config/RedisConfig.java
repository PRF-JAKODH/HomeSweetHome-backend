package com.homesweet.homesweetback.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 설정 파일
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 13.
 */
@Configuration
@EnableRedisRepositories
public class RedisConfig {

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(new StringRedisSerializer());

        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }

    @Bean(name = "stockRedisTemplate")
    public RedisTemplate<String, String> stockRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key와 Value 모두 String(문자열/숫자)으로 처리
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());

        return template;
    }

    /**
     * Event Queue 전용 StringRedisTemplate
     * 단순 문자열 데이터를 위한 템플릿 (JSON 직렬화 불필요)
     */
    @Bean
    public org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate(
            RedisConnectionFactory connectionFactory) {
        return new org.springframework.data.redis.core.StringRedisTemplate(connectionFactory);
    }
}
