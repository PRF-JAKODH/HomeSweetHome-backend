package com.homesweet.homesweetback.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.List;

/**
 * Redis 캐싱 설정
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 15.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @Primary
    public RedisCacheManager redisCacheManager(
            RedisConnectionFactory redisConnectionFactory,
            ObjectMapper objectMapper
    ) {

        ObjectMapper localObjectMapper = objectMapper.copy();
        localObjectMapper.registerModule(new JavaTimeModule());
        localObjectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(localObjectMapper);

        RedisCacheWriter cacheWriter =
                RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofDays(1))
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(serializer)
                );

        return RedisCacheManager.builder(cacheWriter)
                .cacheDefaults(config)
                .build();
    }


    @Bean
    public CacheManager localCacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
                new ConcurrentMapCache("notificationTemplateCache")
        ));
        return cacheManager;
    }
}
