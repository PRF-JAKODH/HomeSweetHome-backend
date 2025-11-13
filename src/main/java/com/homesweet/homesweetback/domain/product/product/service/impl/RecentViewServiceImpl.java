package com.homesweet.homesweetback.domain.product.product.service.impl;

import com.homesweet.homesweetback.domain.product.product.service.RecentViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * 최근 본 상품 서비스 구현체
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 13.
 */
@Service
@RequiredArgsConstructor
public class RecentViewServiceImpl implements RecentViewService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String KEY_PREFIX = "recent:view:";
    private static final long MAX_PRODUCTS = 10;
    private static final Duration TTL = Duration.ofDays(1);

    @Async("recentSearchTaskExecutor")
    @Override
    public void saveView(Long userId, Long productId) {

        String key = getKey(userId);
        double score = System.currentTimeMillis(); // timestamp 기반 score

        // 1. 해당 productId 업데이트 또는 추가
        redisTemplate.opsForZSet().add(key, productId.toString(), score);

        // 2. 최신 10개만 유지
        redisTemplate.opsForZSet().removeRange(key, 0, -(MAX_PRODUCTS + 1));

        // 3. TTL 설정
        redisTemplate.expire(key, TTL);
    }

    @Override
    public List<Long> getRecentViews(Long userId) {
        String key = getKey(userId);

        Set<String> result = redisTemplate.opsForZSet()
                .reverseRange(key, 0, MAX_PRODUCTS - 1);

        return result == null
                ? List.of()
                : result.stream().map(Long::valueOf).toList();
    }

    @Override
    public void deleteOne(Long userId, Long productId) {
        redisTemplate.opsForZSet().remove(getKey(userId), productId.toString());
    }

    @Override
    public void clearAll(Long userId) {
        redisTemplate.delete(getKey(userId));
    }

    private String getKey(Long userId) {
        return KEY_PREFIX + userId;
    }
}
