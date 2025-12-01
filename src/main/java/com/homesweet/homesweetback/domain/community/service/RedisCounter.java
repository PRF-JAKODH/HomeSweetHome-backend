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

    public void incrementCounter(String key) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        // Increment by 1
        ops.increment(key);
    }

    public void decrementCounter(String key) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        ops.decrement(key);
    }

    public boolean addToSet(String key, String member) {
        Long result = redisTemplate.opsForSet().add(key, member);
        return result != null && result > 0;
    }

    public boolean removeFromSet(String key, String member) {
        Long result = redisTemplate.opsForSet().remove(key, member);
        return result != null && result > 0;
    }

    public boolean isMemberOfSet(String key, String member) {
        Boolean result = redisTemplate.opsForSet().isMember(key, member);
        return result != null && result;
    }

    public Long getSetSize(String key){
        return redisTemplate.opsForSet().size(key);
    }
}