package com.homesweet.homesweetback.domain.community.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.apache.logging.log4j.ThreadContext.isEmpty;
import static org.yaml.snakeyaml.tokens.Token.ID.Key;

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

    // set에 데이터가 존재하는지
    public boolean hasKey(String Key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(Key));
    }

    // 한번에 set에 추가 Lazy Loading
    public void addAllToSet(String key, Set<String> members) {
        if (members != null && !members.isEmpty()) {
            redisTemplate.opsForSet().add(key, members.toArray(new String[0]));
            // TTL 설정
            redisTemplate.expire(key, 3, TimeUnit.HOURS);
        }
    }

    // TTL 갱신
    public void expire(String key, long timeout, TimeUnit unit) {
        redisTemplate.expire(key, timeout, unit);
    }

    // 이벤트 큐 적재
    public void pushToQueue(String key, Object value) {
        redisTemplate.opsForList().rightPush(key, value);
    }

    // 큐에서 데이터 가져오기
    public Object popFromQueue(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    // 큐 사이즈
    public Long getQueueSize(String key) {
        return redisTemplate.opsForList().size(key);
    }
}
