package com.homesweet.homesweetback.domain.community.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisCounter {

    private final RedisTemplate<String, Object> redisTemplate;

    public void setCounter(String key, Integer value) {
        redisTemplate.opsForValue().set(key, value); // 초기화
    }
}