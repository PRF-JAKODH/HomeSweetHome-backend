package com.homesweet.homesweetback.domain.community.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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

    public Long getSetSize(String key) {
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

    /**
     * Lua Script를 사용한 원자적 좋아요 토글 + 큐 적재
     * 
     * @param setKey   Redis Set 키 (좋아요 목록)
     * @param member   Set의 멤버 (userId)
     * @param queueKey 이벤트 큐 키
     * @param idPrefix 큐에 저장될 이벤트 접두사 (예: "1:100" -> postId:userId)
     * @return 1이면 추가됨(좋아요), 0이면 제거됨(좋아요 취소)
     */
    public Long toggleSetMemberAndPushEvent(String setKey, String member, String queueKey, String idPrefix) {
        String luaScript = "local setKey = KEYS[1] " +
                "local queueKey = KEYS[2] " +
                "local member = ARGV[1] " +
                "local idPrefix = ARGV[2] " +
                "local isMember = redis.call('SISMEMBER', setKey, member) " +
                "if isMember == 1 then " +
                "    redis.call('SREM', setKey, member) " +
                "    redis.call('RPUSH', queueKey, 'REM:' .. idPrefix) " +
                "    return 0 " +
                "else " +
                "    redis.call('SADD', setKey, member) " +
                "    redis.call('RPUSH', queueKey, 'ADD:' .. idPrefix) " +
                "    return 1 " +
                "end";

        return redisTemplate.execute(
                new DefaultRedisScript<>(luaScript, Long.class),
                Arrays.asList(setKey, queueKey),
                member, idPrefix);
    }
}
