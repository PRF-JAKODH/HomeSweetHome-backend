package com.homesweet.homesweetback.domain.community.scheduler;

import com.homesweet.homesweetback.domain.community.repository.CommunityCommentLikeRepository;
import com.homesweet.homesweetback.domain.community.repository.CommunityPostLikeRepository;
import com.homesweet.homesweetback.domain.community.repository.CommunityPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class Scheduler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;
    private final CommunityPostRepository communityPostRepository;
    private final CommunityPostLikeRepository postLikeRepository;
    private final CommunityCommentLikeRepository commentLikeRepository;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(initialDelay = 500000, fixedDelay = 500000)
    public void updateCountData() {
        //  scan으로 변경
        ScanOptions options = ScanOptions.scanOptions()
                .match("post:*:viewCount")
                .count(100)
                .build();

        int successCount = 0;
        int failCount = 0;

        try (Cursor<String> cursor = redisTemplate.scan(options)) {

            //  각 key마다 반복
            while(cursor.hasNext()) {
                String key = cursor.next();

                try {
                    //  key에서 postId 추출
                    String[] parts = key.split(":");
                    Long postId = Long.parseLong(parts[1]);

                    // 키에 대한 밸류 가져옴 redis에서
                    Integer viewCount = (Integer) redisTemplate.opsForValue().get(key);
                    if (viewCount == null) continue;

                    // db저장 (짧은 트랜잭션으로 커넥션 즉시 반환)
                    Boolean updated = transactionTemplate.execute(status -> {
                        if (communityPostRepository.findByPostIdAndIsDeletedFalse(postId).isPresent()) {
                            communityPostRepository.updateViewCount(postId, viewCount);
                            return true;
                        }
                        return false;
                    });

                    if (Boolean.TRUE.equals(updated)) {
                        redisTemplate.delete(key);
                        successCount++;
                    } else {
                        // 게시글이 삭제된 경우에도 Redis 키 정리
                        redisTemplate.delete(key);
                    }
                } catch (Exception e) {
                    log.error("Failed to update view count for key: {}", key, e);
                    failCount++;
                    // 실패한 건은 Redis에 남겨서 다음 실행 시 재시도
                }
            }
        } catch (Exception e) {
            log.error("Failed to scan Redis for view counts", e);
        }

        log.info("View count sync completed - success: {}, failed: {}", successCount, failCount);
    }

    // 댓글 수정
    @Scheduled(initialDelay = 200000, fixedDelay = 200000)
    public void updateCommentData() {
        //  Redis에서 모든 조회수 key 찾기
        ScanOptions options = ScanOptions.scanOptions()
                .match("post:*:commentCount")
                .count(100)
                .build();

        int successCount = 0;
        int failCount = 0;

        try (Cursor<String> cursor = redisTemplate.scan(options)) {

            while (cursor.hasNext()) {
                String key = cursor.next();

                try {
                    String[] parts = key.split(":");
                    Long postId = Long.parseLong(parts[1]);
                    Integer commentCount = (Integer) redisTemplate.opsForValue().get(key);
                    if (commentCount == null) continue;

                    // db저장 (짧은 트랜잭션으로 커넥션 즉시 반환)
                    Boolean updated = transactionTemplate.execute(status -> {
                        if (communityPostRepository.findByPostIdAndIsDeletedFalse(postId).isPresent()) {
                            communityPostRepository.setCommentCount(postId, commentCount);
                            return true;
                        }
                        return false;
                    });

                    if (Boolean.TRUE.equals(updated)) {
                        redisTemplate.delete(key);
                        successCount++;
                    } else {
                        // 게시글이 삭제된 경우에도 Redis 키 정리
                        redisTemplate.delete(key);
                    }
                } catch (Exception e) {
                    log.error("Failed to update comment count for key: {}", key, e);
                    failCount++;
                    // 실패한 건은 Redis에 남겨서 다음 실행 시 재시도
                }
            }
        } catch (Exception e) {
            log.error("Failed to scan Redis for comment counts", e);
        }

        log.info("Comment count sync completed - success: {}, failed: {}", successCount, failCount);
    }

    /**
     * 게시글 좋아요 Event Queue 배치 동기화
     * 5분마다 실행
     */
    @Scheduled(fixedDelay = 300000)  // 5분
    public void syncPostLikeEvents() {
        String queueKey = "post:like:events";

        try {
            // Queue에서 최대 1000개 이벤트 가져오기
            List<String> events = stringRedisTemplate.opsForList().range(queueKey, 0, 999);
            if (events == null || events.isEmpty()) {
                return;
            }

            // postId별로 마지막 상태 추적
            Map<String, String> lastEventMap = new HashMap<>();
            for (String event : events) {  // "postId:userId:action"
                String[] parts = event.split(":");
                if (parts.length == 3) {
                    String key = parts[0] + ":" + parts[1];  // postId:userId
                    lastEventMap.put(key, parts[2]);  // ADD or REMOVE
                }
            }

            // DB에 반영
            int addCount = 0;
            int removeCount = 0;
            int failCount = 0;

            for (Map.Entry<String, String> entry : lastEventMap.entrySet()) {
                try {
                    String[] key = entry.getKey().split(":");
                    Long postId = Long.parseLong(key[0]);
                    Long userId = Long.parseLong(key[1]);
                    String action = entry.getValue();

                    // 짧은 트랜잭션으로 커넥션 즉시 반환
                    transactionTemplate.executeWithoutResult(status -> {
                        if ("ADD".equals(action)) {
                            postLikeRepository.insertPostLike(postId, userId);
                        } else if ("REMOVE".equals(action)) {
                            postLikeRepository.deleteByPostIdAndUserId(postId, userId);
                        }
                    });

                    if ("ADD".equals(action)) {
                        addCount++;
                    } else if ("REMOVE".equals(action)) {
                        removeCount++;
                    }
                } catch (Exception e) {
                    log.error("Failed to sync post like event: {}", entry, e);
                    failCount++;
                }
            }

            // 처리된 이벤트 제거 (실패한 것 포함 - INSERT IGNORE로 멱등성 보장됨)
            stringRedisTemplate.opsForList().trim(queueKey, events.size(), -1);

            log.info("Post like events synced - total: {}, added: {}, removed: {}, failed: {}",
                    events.size(), addCount, removeCount, failCount);
        } catch (Exception e) {
            log.error("Failed to sync post like events", e);
        }
    }

    /**
     * 댓글 좋아요 Event Queue 배치 동기화
     * 5분마다 실행
     */
    @Scheduled(fixedDelay = 300000)  // 5분
    public void syncCommentLikeEvents() {
        String queueKey = "comment:like:events";

        try {
            // Queue에서 최대 1000개 이벤트 가져오기
            List<String> events = stringRedisTemplate.opsForList().range(queueKey, 0, 999);
            if (events == null || events.isEmpty()) {
                return;
            }

            // commentId별로 마지막 상태 추적
            Map<String, String> lastEventMap = new HashMap<>();
            for (String event : events) {  // "commentId:userId:action"
                String[] parts = event.split(":");
                if (parts.length == 3) {
                    String key = parts[0] + ":" + parts[1];  // commentId:userId
                    lastEventMap.put(key, parts[2]);  // ADD or REMOVE
                }
            }

            // DB에 반영
            int addCount = 0;
            int removeCount = 0;
            int failCount = 0;

            for (Map.Entry<String, String> entry : lastEventMap.entrySet()) {
                try {
                    String[] key = entry.getKey().split(":");
                    Long commentId = Long.parseLong(key[0]);
                    Long userId = Long.parseLong(key[1]);
                    String action = entry.getValue();

                    // 짧은 트랜잭션으로 커넥션 즉시 반환
                    transactionTemplate.executeWithoutResult(status -> {
                        if ("ADD".equals(action)) {
                            commentLikeRepository.insertCommentLike(commentId, userId);
                        } else if ("REMOVE".equals(action)) {
                            commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);
                        }
                    });

                    if ("ADD".equals(action)) {
                        addCount++;
                    } else if ("REMOVE".equals(action)) {
                        removeCount++;
                    }
                } catch (Exception e) {
                    log.error("Failed to sync comment like event: {}", entry, e);
                    failCount++;
                }
            }

            // 처리된 이벤트 제거 (실패한 것 포함 - INSERT IGNORE로 멱등성 보장됨)
            stringRedisTemplate.opsForList().trim(queueKey, events.size(), -1);

            log.info("Comment like events synced - total: {}, added: {}, removed: {}, failed: {}",
                    events.size(), addCount, removeCount, failCount);
        } catch (Exception e) {
            log.error("Failed to sync comment like events", e);
        }
    }
}