package com.homesweet.homesweetback.domain.community.scheduler;

import com.homesweet.homesweetback.domain.community.repository.CommunityCommentLikeRepository;
import com.homesweet.homesweetback.domain.community.repository.CommunityPostLikeRepository;
import com.homesweet.homesweetback.domain.community.repository.CommunityPostRepository;
import com.homesweet.homesweetback.domain.community.service.RedisCounter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class Scheduler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisCounter redisCounter; // 추가
    private final CommunityPostRepository communityPostRepository;
    private final CommunityPostLikeRepository postLikeRepository;
    private final CommunityCommentLikeRepository commentLikeRepository;

    private static final String POST_LIKE_EVENT_QUEUE = "post:like:events";
    private static final String COMMENT_LIKE_EVENT_QUEUE = "comment:like:events";
    private static final int BATCH_SIZE = 500; // 한 번에 처리할 이벤트 수

    // 1. 조회수 Sync (기존 유지)
    @Transactional
    @Scheduled(initialDelay = 1000000, fixedDelay = 1000000)
    public void updateCountData() {
        ScanOptions options = ScanOptions.scanOptions()
                .match("post:*:viewCount")
                .count(100)
                .build();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                String[] parts = key.split(":");
                Long postId = Long.parseLong(parts[1]);
                Integer viewCount = (Integer) redisTemplate.opsForValue().get(key);
                if (viewCount == null)
                    continue;

                if (communityPostRepository.findByPostIdAndIsDeletedFalse(postId).isPresent()) {
                    communityPostRepository.updateViewCount(postId, viewCount);
                }
                redisTemplate.delete(key);
            }
        }
    }

    // 2. 댓글수 Sync (기존 유지)
    @Transactional
    @Scheduled(initialDelay = 1500000, fixedDelay = 1500000)
    public void updateCommentData() {
        ScanOptions options = ScanOptions.scanOptions()
                .match("post:*:commentCount")
                .count(100)
                .build();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                String[] parts = key.split(":");
                Long postId = Long.parseLong(parts[1]);
                Integer commentCount = (Integer) redisTemplate.opsForValue().get(key);
                if (commentCount == null)
                    continue;

                if (communityPostRepository.findByPostIdAndIsDeletedFalse(postId).isPresent()) {
                    communityPostRepository.setCommentCount(postId, commentCount);
                }
                redisTemplate.delete(key);
            }
        }
    }

    // 3. [New] 게시글 좋아요 DB Sync (자주 실행: 10초)
    @Transactional
    @Scheduled(fixedDelay = 10000)
    public void syncPostLikeEvents() {
        processLikeEvents(POST_LIKE_EVENT_QUEUE, true);
    }

    // 4. [New] 댓글 좋아요 DB Sync (자주 실행: 10초)
    @Transactional
    @Scheduled(fixedDelay = 10000)
    public void syncCommentLikeEvents() {
        processLikeEvents(COMMENT_LIKE_EVENT_QUEUE, false);
    }

    // 좋아요 이벤트 처리 공통 로직 (중복 제거 추가)
    private void processLikeEvents(String queueKey, boolean isPost) {
        Long size = redisCounter.getQueueSize(queueKey);
        if (size == null || size == 0)
            return;

        // ✅ 중복 제거용 Map (postId:userId → 최종 액션)
        Map<String, String> latestActions = new LinkedHashMap<>();

        // BATCH_SIZE만큼 꺼내서 중복 제거
        for (int i = 0; i < BATCH_SIZE && i < size; i++) {
            Object eventObj = redisCounter.popFromQueue(queueKey);
            if (eventObj == null)
                break;

            String event = (String) eventObj;
            String[] parts = event.split(":"); // "ADD:1:100"
            if (parts.length != 3) {
                log.warn("Invalid event format: {}", event);
                continue;
            }

            String key = parts[1] + ":" + parts[2]; // "postId:userId"
            // ✅ 마지막 액션만 저장 (중복 덮어쓰기)
            latestActions.put(key, event);
        }

        // ✅ 중복 제거된 이벤트만 처리
        for (String event : latestActions.values()) {
            String[] parts = event.split(":");
            String action = parts[0];
            Long id = Long.parseLong(parts[1]);
            Long userId = Long.parseLong(parts[2]);

            try {
                if (isPost) {
                    if ("ADD".equals(action))
                        postLikeRepository.insertPostLike(id, userId);
                    else if ("REM".equals(action))
                        postLikeRepository.deleteByPostIdAndUserId(id, userId);
                } else {
                    if ("ADD".equals(action))
                        commentLikeRepository.insertCommentLike(id, userId);
                    else if ("REM".equals(action))
                        commentLikeRepository.deleteByCommentIdAndUserId(id, userId);
                }
            } catch (Exception e) {
                log.error("Failed to sync like event: {} (Queue: {})", event, queueKey, e);
                // 실패한 건 로그만 남기고 넘어감 (Eventually Consistent)
                // 필요 시 Dead Letter Queue에 넣는 로직 추가 가능
            }
        }
    }
}