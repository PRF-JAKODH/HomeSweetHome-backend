package com.homesweet.homesweetback.domain.community.scheduler;

import com.homesweet.homesweetback.domain.community.repository.CommunityCommentLikeRepository;
import com.homesweet.homesweetback.domain.community.repository.CommunityCommentRepository;
import com.homesweet.homesweetback.domain.community.repository.CommunityPostLikeRepository;
import com.homesweet.homesweetback.domain.community.repository.CommunityPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class CommunityScheduler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;
    private final CommunityPostRepository communityPostRepository;
    private final CommunityPostLikeRepository postLikeRepository;
    private final CommunityCommentLikeRepository commentLikeRepository;
    private final CommunityCommentRepository commentRepository;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(initialDelay = 500000, fixedDelay = 500000)
    public void updateCountData() {
        ScanOptions options = ScanOptions.scanOptions()
                .match("post:*:viewCount")
                .count(100)
                .build();

        // Redis에서 업데이트할 데이터 수집
        Map<Long, Integer> updatesToApply = new HashMap<>();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while(cursor.hasNext()) {
                String key = cursor.next();

                try {
                    String[] parts = key.split(":");
                    Long postId = Long.parseLong(parts[1]);
                    Integer viewCount = (Integer) redisTemplate.opsForValue().get(key);

                    if (viewCount != null) {
                        updatesToApply.put(postId, viewCount);
                    }
                } catch (Exception e) {
                    log.error("Failed to parse Redis key: {}", key, e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to scan Redis for view counts", e);
        }

        // 하나의 트랜잭션으로 모든 업데이트 처리
        int successCount = 0;
        int failCount = 0;

        if (!updatesToApply.isEmpty()) {
            transactionTemplate.executeWithoutResult(status -> {
                for (Map.Entry<Long, Integer> entry : updatesToApply.entrySet()) {
                    Long postId = entry.getKey();
                    Integer viewCount = entry.getValue();

                    try {
                        // updateViewCount는 affected rows를 반환 (0이면 게시글 없음)
                        int affected = communityPostRepository.updateViewCount(postId, viewCount);

                        if (affected > 0) {
                            redisTemplate.delete("post:" + postId + ":viewCount");
                        } else {
                            // 게시글이 삭제된 경우에도 Redis 키 정리
                            redisTemplate.delete("post:" + postId + ":viewCount");
                        }
                    } catch (Exception e) {
                        log.error("Failed to update view count for postId: {}", postId, e);
                    }
                }
            });

            // 성공/실패 카운트 계산
            successCount = updatesToApply.size();
        }

        log.info("View count sync completed - total processed: {}", successCount);
    }

    // 댓글 수정
    @Scheduled(initialDelay = 200000, fixedDelay = 200000)
    public void updateCommentData() {
        ScanOptions options = ScanOptions.scanOptions()
                .match("post:*:commentCount")
                .count(100)
                .build();

        // Redis에서 업데이트할 데이터 수집
        Map<Long, Integer> updatesToApply = new HashMap<>();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();

                try {
                    String[] parts = key.split(":");
                    Long postId = Long.parseLong(parts[1]);
                    Integer commentCount = (Integer) redisTemplate.opsForValue().get(key);

                    if (commentCount != null) {
                        updatesToApply.put(postId, commentCount);
                    }
                } catch (Exception e) {
                    log.error("Failed to parse Redis key: {}", key, e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to scan Redis for comment counts", e);
        }

        // 하나의 트랜잭션으로 모든 업데이트 처리
        int successCount = 0;

        if (!updatesToApply.isEmpty()) {
            transactionTemplate.executeWithoutResult(status -> {
                for (Map.Entry<Long, Integer> entry : updatesToApply.entrySet()) {
                    Long postId = entry.getKey();
                    Integer commentCount = entry.getValue();

                    try {
                        // setCommentCount는 affected rows를 반환 (0이면 게시글 없음)
                        int affected = communityPostRepository.setCommentCount(postId, commentCount);

                        if (affected > 0) {
                            redisTemplate.delete("post:" + postId + ":commentCount");
                        } else {
                            // 게시글이 삭제된 경우에도 Redis 키 정리
                            redisTemplate.delete("post:" + postId + ":commentCount");
                        }
                    } catch (Exception e) {
                        log.error("Failed to update comment count for postId: {}", postId, e);
                    }
                }
            });

            successCount = updatesToApply.size();
        }

        log.info("Comment count sync completed - total processed: {}", successCount);
    }

    /**
     * 게시글 좋아요 Event Queue 배치 동기화 (관계 + 개수)
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

            // 하나의 트랜잭션으로 모든 이벤트 처리
            int addCount = 0;
            int removeCount = 0;

            if (!lastEventMap.isEmpty()) {
                transactionTemplate.executeWithoutResult(status -> {
                    for (Map.Entry<String, String> entry : lastEventMap.entrySet()) {
                        try {
                            String[] key = entry.getKey().split(":");
                            Long postId = Long.parseLong(key[0]);
                            Long userId = Long.parseLong(key[1]);
                            String action = entry.getValue();

                            if ("ADD".equals(action)) {
                                postLikeRepository.insertPostLike(postId, userId);
                            } else if ("REMOVE".equals(action)) {
                                postLikeRepository.deleteByPostIdAndUserId(postId, userId);
                            }
                        } catch (Exception e) {
                            log.error("Failed to sync post like event: {}", entry, e);
                        }
                    }
                });

                // 카운트 계산
                for (String action : lastEventMap.values()) {
                    if ("ADD".equals(action)) {
                        addCount++;
                    } else if ("REMOVE".equals(action)) {
                        removeCount++;
                    }
                }
            }

            // 처리된 이벤트 제거 (실패한 것 포함 - INSERT IGNORE로 멱등성 보장됨)
            stringRedisTemplate.opsForList().trim(queueKey, events.size(), -1);

            log.info("Post like events synced - total: {}, added: {}, removed: {}",
                    events.size(), addCount, removeCount);
        } catch (Exception e) {
            log.error("Failed to sync post like events", e);
        }
    }

    /**
     * 게시글 좋아요 개수 배치 동기화 (Redis → DB)
     * 5분마다 실행 (좋아요 이벤트 동기화 직후)
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 310000)  // 5분, 초기 지연 5분 10초
    public void syncPostLikeCount() {
        ScanOptions options = ScanOptions.scanOptions()
                .match("post:*:likeCount")
                .count(100)
                .build();

        // Redis에서 업데이트할 데이터 수집
        Map<Long, Integer> updatesToApply = new HashMap<>();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();

                try {
                    String[] parts = key.split(":");
                    Long postId = Long.parseLong(parts[1]);
                    Integer likeCount = (Integer) redisTemplate.opsForValue().get(key);

                    if (likeCount != null) {
                        updatesToApply.put(postId, likeCount);
                    }
                } catch (Exception e) {
                    log.error("Failed to parse Redis key: {}", key, e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to scan Redis for like counts", e);
        }

        // 하나의 트랜잭션으로 모든 업데이트 처리
        int successCount = 0;

        if (!updatesToApply.isEmpty()) {
            transactionTemplate.executeWithoutResult(status -> {
                for (Map.Entry<Long, Integer> entry : updatesToApply.entrySet()) {
                    Long postId = entry.getKey();
                    Integer likeCount = entry.getValue();

                    try {
                        // setLikeCount는 affected rows를 반환 (0이면 게시글 없음)
                        int affected = communityPostRepository.setLikeCount(postId, likeCount);

                        if (affected > 0) {
                            // DB 동기화 성공 시 Redis 값 유지 (삭제하지 않음 - Cache-Aside 패턴)
                            log.debug("Post like count synced to DB - postId: {}, likeCount: {}", postId, likeCount);
                        } else {
                            // 게시글이 삭제된 경우 Redis 키 정리
                            redisTemplate.delete("post:" + postId + ":likeCount");
                            redisTemplate.delete("post:" + postId + ":likes");
                        }
                    } catch (Exception e) {
                        log.error("Failed to update like count for postId: {}", postId, e);
                    }
                }
            });

            successCount = updatesToApply.size();
        }

        log.info("Post like count sync completed - total processed: {}", successCount);
    }

    /**
     * 댓글 좋아요 Event Queue 배치 동기화 (관계)
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

            // 하나의 트랜잭션으로 모든 이벤트 처리
            int addCount = 0;
            int removeCount = 0;

            if (!lastEventMap.isEmpty()) {
                transactionTemplate.executeWithoutResult(status -> {
                    for (Map.Entry<String, String> entry : lastEventMap.entrySet()) {
                        try {
                            String[] key = entry.getKey().split(":");
                            Long commentId = Long.parseLong(key[0]);
                            Long userId = Long.parseLong(key[1]);
                            String action = entry.getValue();

                            if ("ADD".equals(action)) {
                                commentLikeRepository.insertCommentLike(commentId, userId);
                            } else if ("REMOVE".equals(action)) {
                                commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);
                            }
                        } catch (Exception e) {
                            log.error("Failed to sync comment like event: {}", entry, e);
                        }
                    }
                });

                // 카운트 계산
                for (String action : lastEventMap.values()) {
                    if ("ADD".equals(action)) {
                        addCount++;
                    } else if ("REMOVE".equals(action)) {
                        removeCount++;
                    }
                }
            }

            // 처리된 이벤트 제거 (실패한 것 포함 - INSERT IGNORE로 멱등성 보장됨)
            stringRedisTemplate.opsForList().trim(queueKey, events.size(), -1);

            log.info("Comment like events synced - total: {}, added: {}, removed: {}",
                    events.size(), addCount, removeCount);
        } catch (Exception e) {
            log.error("Failed to sync comment like events", e);
        }
    }

    /**
     * 댓글 좋아요 개수 배치 동기화 (Redis → DB)
     * 5분마다 실행 (댓글 좋아요 이벤트 동기화 직후)
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 320000)  // 5분, 초기 지연 5분 20초
    public void syncCommentLikeCount() {
        ScanOptions options = ScanOptions.scanOptions()
                .match("comment:*:likeCount")
                .count(100)
                .build();

        // Redis에서 업데이트할 데이터 수집
        Map<Long, Integer> updatesToApply = new HashMap<>();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();

                try {
                    String[] parts = key.split(":");
                    Long commentId = Long.parseLong(parts[1]);
                    Integer likeCount = (Integer) redisTemplate.opsForValue().get(key);

                    if (likeCount != null) {
                        updatesToApply.put(commentId, likeCount);
                    }
                } catch (Exception e) {
                    log.error("Failed to parse Redis key: {}", key, e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to scan Redis for comment like counts", e);
        }

        // 하나의 트랜잭션으로 모든 업데이트 처리
        int successCount = 0;

        if (!updatesToApply.isEmpty()) {
            transactionTemplate.executeWithoutResult(status -> {
                for (Map.Entry<Long, Integer> entry : updatesToApply.entrySet()) {
                    Long commentId = entry.getKey();
                    Integer likeCount = entry.getValue();

                    try {
                        // setLikeCount는 affected rows를 반환 (0이면 댓글 없음)
                        int affected = commentRepository.setLikeCount(commentId, likeCount);

                        if (affected > 0) {
                            // DB 동기화 성공 시 Redis 값 유지 (삭제하지 않음 - Cache-Aside 패턴)
                            log.debug("Comment like count synced to DB - commentId: {}, likeCount: {}", commentId, likeCount);
                        } else {
                            // 댓글이 삭제된 경우 Redis 키 정리
                            redisTemplate.delete("comment:" + commentId + ":likeCount");
                            redisTemplate.delete("comment:" + commentId + ":likes");
                        }
                    } catch (Exception e) {
                        log.error("Failed to update comment like count for commentId: {}", commentId, e);
                    }
                }
            });

            successCount = updatesToApply.size();
        }

        log.info("Comment like count sync completed - total processed: {}", successCount);
    }
}