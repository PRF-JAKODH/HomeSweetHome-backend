package com.homesweet.homesweetback.domain.product.product.service.impl;

import com.homesweet.homesweetback.common.util.JsonUtil;
import com.homesweet.homesweetback.domain.product.product.controller.response.ProductDetailResponse;
import com.homesweet.homesweetback.domain.product.product.controller.response.RecentViewPreviewResponse;
import com.homesweet.homesweetback.domain.product.product.service.RecentViewService;
import lombok.RequiredArgsConstructor;
import org.flywaydb.core.internal.util.JsonUtils;
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

    private static final String VIEW_KEY_PREFIX = "recent:view:";
    private static final String PREVIEW_KEY_PREFIX = "product:preview:";

    private static final long MAX_PRODUCTS = 10;
    private static final Duration VIEW_TTL = Duration.ofDays(1);
    private static final Duration PREVIEW_TTL = Duration.ofHours(12);

    @Async("recentSearchTaskExecutor")
    @Override
    public void saveView(Long userId, Long productId) {
        String key = VIEW_KEY_PREFIX + userId;
        double score = System.currentTimeMillis(); // 최신순 정렬

        redisTemplate.opsForZSet().add(key, productId.toString(), score);

        redisTemplate.opsForZSet().removeRange(key, 0, -(MAX_PRODUCTS + 1));

        redisTemplate.expire(key, VIEW_TTL);
    }

    // 최근 본 상품 캐싱
    @Async("recentSearchTaskExecutor")
    @Override
    public void cachePreview(Long productId, ProductDetailResponse detail) {
        redisTemplate.opsForValue().set(
                PREVIEW_KEY_PREFIX + productId,
                JsonUtils.toJson(RecentViewPreviewResponse.fromDetail(detail)),
                PREVIEW_TTL
        );
    }

    @Override
    public List<Long> getRecentViewsIds(Long userId) {
        String key = VIEW_KEY_PREFIX + userId;

        Set<String> result = redisTemplate.opsForZSet()
                .reverseRange(key, 0, MAX_PRODUCTS - 1);

        return result == null
                ? List.of()
                : result.stream().map(Long::valueOf).toList();
    }

    @Override
    public RecentViewPreviewResponse getCachedPreview(Long productId) {

        String json = redisTemplate.opsForValue()
                .get(PREVIEW_KEY_PREFIX + productId);

        if (json == null) return null;

        return JsonUtil.fromJson(json, RecentViewPreviewResponse.class);
    }

    @Override
    public void deleteOne(Long userId, Long productId) {
        redisTemplate.opsForZSet().remove(
                VIEW_KEY_PREFIX + userId,
                productId.toString()
        );
    }

    @Override
    public void clearAll(Long userId) {
        redisTemplate.delete(VIEW_KEY_PREFIX + userId);
    }
}
