<<<<<<<< HEAD:src/main/java/com/homesweet/homesweetback/domain/product/recent/service/impl/RecentSearchServiceImpl.java
package com.homesweet.homesweetback.domain.product.recent.service.impl;

import com.homesweet.homesweetback.domain.product.recent.service.RecentSearchService;
========
package com.homesweet.homesweetback.domain.product.product.command.service.impl;

import com.homesweet.homesweetback.domain.product.product.command.service.RecentSearchService;
>>>>>>>> 9de1dca (feat: CQRS에 맞는 폴더 구조 설정):src/main/java/com/homesweet/homesweetback/domain/product/product/command/service/impl/RecentSearchServiceImpl.java
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 최근 검색 조회 서비스 구현체
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 13.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecentSearchServiceImpl implements RecentSearchService {

    private final RedisTemplate<String, String> stringRedisTemplate;

    private static final String KEY_PREFIX = "recent:search:";
    private static final int MAX_KEYWORDS = 10;
    private static final Duration TTL = Duration.ofDays(1);

    // 최근 검색어 저장
    @Async("recentSearchTaskExecutor")
    @Override
    public void save(Long userId, String keyword) {

        String key = getKey(userId);

        // 1; 기존 동일 키워드 제거 → 중복 제거
        stringRedisTemplate.opsForList().remove(key, 0, keyword);

        // 2. 맨 앞에 삽입
        stringRedisTemplate.opsForList().leftPush(key, keyword);

        // 3. MAX_KEYWORDS 개수 유지
        stringRedisTemplate.opsForList().trim(key, 0, MAX_KEYWORDS - 1);

        stringRedisTemplate.expire(key, TTL);

    }

    // 최근 검색어 조회
    @Override
    public List<String> getRecent(Long userId) {
        return stringRedisTemplate.opsForList()
                .range(getKey(userId), 0, MAX_KEYWORDS - 1);
    }

    // 최근 검색어 단일 제거
    @Override
    public void deleteKeyword(Long userId, String keyword) {
        stringRedisTemplate.opsForList().remove(getKey(userId), 0, keyword);
    }

    // 최근 검색어 전체 제거
    @Override
    public void clearAll(Long userId) {
        stringRedisTemplate.delete(getKey(userId));
    }

    private String getKey(Long userId) {
        return KEY_PREFIX + userId;
    }

}
