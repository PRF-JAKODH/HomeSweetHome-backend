package com.homesweet.homesweetback.domain.community.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis 기반 커뮤니티 카운터 서비스
 * - 조회수, 좋아요 등의 카운터를 Redis에서 관리
 * - 원자적 연산으로 동시성 제어 보장
 * - 주기적으로 DB에 동기화 (배치 작업)
 *
 * @author ohhalim777@gmail.com
 * @date 25. 11. 17.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCommunityCountService {

    private final RedisTemplate<String, Long> redisTemplate;

    // Redis Key 패턴
    private static final String VIEW_COUNT_KEY = "community:post:view:%d";
    private static final String LIKE_COUNT_KEY = "community:post:like:%d";
    private static final String COMMENT_COUNT_KEY = "community:post:comment:%d";

    /**
     * 조회수 증가 (Redis)
     * 원자적 연산으로 동시성 보장
     */
    public void incrementViewCount(Long postId) {
        String key = String.format(VIEW_COUNT_KEY, postId);
        redisTemplate.opsForValue().increment(key);
        log.debug("Redis 조회수 증가: postId={}", postId);
    }

    /**
     * 좋아요 증가 (Redis)
     */
    public void incrementLikeCount(Long postId) {
        String key = String.format(LIKE_COUNT_KEY, postId);
        redisTemplate.opsForValue().increment(key);
        log.debug("Redis 좋아요 증가: postId={}", postId);
    }

    /**
     * 좋아요 감소 (Redis)
     */
    public void decrementLikeCount(Long postId) {
        String key = String.format(LIKE_COUNT_KEY, postId);
        redisTemplate.opsForValue().decrement(key);
        log.debug("Redis 좋아요 감소: postId={}", postId);
    }

    /**
     * 댓글 카운트 증가 (Redis)
     */
    public void incrementCommentCount(Long postId) {
        String key = String.format(COMMENT_COUNT_KEY, postId);
        redisTemplate.opsForValue().increment(key);
        log.debug("Redis 댓글 카운트 증가: postId={}", postId);
    }

    /**
     * 댓글 카운트 감소 (Redis)
     */
    public void decrementCommentCount(Long postId) {
        String key = String.format(COMMENT_COUNT_KEY, postId);
        redisTemplate.opsForValue().decrement(key);
        log.debug("Redis 댓글 카운트 감소: postId={}", postId);
    }

    /**
     * Redis의 카운트 조회
     */
    public Long getViewCount(Long postId) {
        String key = String.format(VIEW_COUNT_KEY, postId);
        Long count = redisTemplate.opsForValue().get(key);
        return count != null ? count : 0L;
    }

    public Long getLikeCount(Long postId) {
        String key = String.format(LIKE_COUNT_KEY, postId);
        Long count = redisTemplate.opsForValue().get(key);
        return count != null ? count : 0L;
    }

    public Long getCommentCount(Long postId) {
        String key = String.format(COMMENT_COUNT_KEY, postId);
        Long count = redisTemplate.opsForValue().get(key);
        return count != null ? count : 0L;
    }

    /**
     * Redis 카운트 삭제 (DB 동기화 후)
     */
    public void deleteViewCount(Long postId) {
        String key = String.format(VIEW_COUNT_KEY, postId);
        redisTemplate.delete(key);
    }

    public void deleteLikeCount(Long postId) {
        String key = String.format(LIKE_COUNT_KEY, postId);
        redisTemplate.delete(key);
    }

    public void deleteCommentCount(Long postId) {
        String key = String.format(COMMENT_COUNT_KEY, postId);
        redisTemplate.delete(key);
    }
}
