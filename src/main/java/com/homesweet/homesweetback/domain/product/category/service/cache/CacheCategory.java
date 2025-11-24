package com.homesweet.homesweetback.domain.product.category.service.cache;

import com.homesweet.homesweetback.domain.product.category.repository.ProductCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 카테고리 캐싱 서비스
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 18.
 */
@Service
@RequiredArgsConstructor
public class CacheCategory {

    private static final String KEY_PREFIX = "category:children:";

    private final ProductCategoryRepository categoryRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 특정 카테고리의 모든 하위 카테고리 ID 반환 (캐싱 적용)
     */
    public List<Long> getAllSubCategoryIds(Long categoryId) {
        String key = KEY_PREFIX + categoryId;

        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return (List<Long>) cached;
        }

        List<Long> categoryIds = categoryRepository.findAllSubCategoryIds(categoryId);

        // 3. Redis에 저장
        redisTemplate.opsForValue().set(key, categoryIds);

        return categoryIds;
    }

    /**
     * 특정 카테고리 캐시 삭제 (관리자 페이지에서 카테고리 수정 시 호출)
     */
    public void evictCategoryCache(Long categoryId) {
        redisTemplate.delete(KEY_PREFIX + categoryId);
    }

    /**
     * 모든 카테고리 캐시 삭제 (대규모 구조 변경 시)
     */
    public void evictAllCategoryCaches() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
