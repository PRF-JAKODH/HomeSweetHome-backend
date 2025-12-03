package com.homesweet.homesweetback.domain.community.service;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.community.entity.CommunityCommentEntity;
import com.homesweet.homesweetback.domain.community.entity.CommunityPostEntity;
import com.homesweet.homesweetback.domain.community.exception.CommunityException;
import com.homesweet.homesweetback.domain.community.repository.CommunityCommentLikeRepository;
import com.homesweet.homesweetback.domain.community.repository.CommunityCommentRepository;
import com.homesweet.homesweetback.domain.community.repository.CommunityPostLikeRepository;
import com.homesweet.homesweetback.domain.community.repository.CommunityPostRepository;
import com.homesweet.homesweetback.domain.notification.domain.notification.CommunityNotification;
import com.homesweet.homesweetback.domain.notification.service.NotificationSendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * Community Count 서비스
 *
 * @author ohhalim777@gmail.com
 * @date 25. 10. 21.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityCountService {

    private final RedisCounter redisCounter;
    private final CommunityPostRepository postRepository;
    private final CommunityPostLikeRepository postLikeRepository;
    private final CommunityCommentRepository commentRepository;
    private final CommunityCommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;
    private final NotificationSendService notificationSendService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    // Lua Script: 좋아요 토글 (원자적 처리)
    private static final String TOGGLE_LIKE_SCRIPT =
            "if redis.call('EXISTS', KEYS[1]) == 0 then " +
            "  return -1 " +
            "end " +
            "local isMember = redis.call('SISMEMBER', KEYS[1], ARGV[1]) " +
            "if isMember == 1 then " +
            "  redis.call('SREM', KEYS[1], ARGV[1]) " +
            "  redis.call('DECR', KEYS[2]) " +
            "  return 0 " +
            "else " +
            "  redis.call('SADD', KEYS[1], ARGV[1]) " +
            "  redis.call('INCR', KEYS[2]) " +
            "  return 1 " +
            "end";

    // Lua Script: 카운터 증가 (원자적 처리)
    private static final String INCREMENT_COUNTER_SCRIPT =
            "if redis.call('EXISTS', KEYS[1]) == 0 then " +
            "  return -1 " +
            "end " +
            "return redis.call('INCR', KEYS[1])";

    // Lua Script: 카운터 증감 (원자적 처리)
    private static final String UPDATE_COUNTER_SCRIPT =
            "if redis.call('EXISTS', KEYS[1]) == 0 then " +
            "  return -1 " +
            "end " +
            "return redis.call('INCRBY', KEYS[1], ARGV[1])";

    /**
     * 조회수 초기화
     */
    @Transactional
    public void initViewCountFromDB(Long postId){
        CommunityPostEntity communityPostEntity = postRepository.findByPostIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new CommunityException(ErrorCode.COMMUNITY_POST_NOT_FOUND));

        String key = "post:" + postId + ":viewCount";
        redisCounter.setCounter(key, communityPostEntity.getViewCount());
    }

    // 조회수 증가 - Lua Script 방식 (Race Condition 해결)
    @Transactional
    public void increaseViewCount(Long postId) {
        String key = "post:" + postId + ":viewCount";

        // Lua Script 실행 (원자적 처리)
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(INCREMENT_COUNTER_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, Arrays.asList(key));

        // Cache Miss 처리 (Lazy Loading)
        if (result == -1) {
            initViewCountFromDB(postId);
            redisTemplate.execute(script, Arrays.asList(key));
        }

        log.debug("View count increased - postId: {}", postId);
    }

    // 댓글수 초기화
    @Transactional
    public void initCommentCountFromDB(Long postId){
        CommunityPostEntity communityPostEntity = postRepository.findByPostIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new CommunityException(ErrorCode.COMMUNITY_POST_NOT_FOUND));

        String key = "post:" + postId + ":commentCount";
        redisCounter.setCounter(key, communityPostEntity.getCommentCount());
    }

    // 댓글수 증가 - Lua Script 방식 (Race Condition 해결)
    @Transactional
    public void increaseCommentCount(Long postId) {
        String key = "post:" + postId + ":commentCount";

        // Lua Script 실행 (원자적 처리)
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(INCREMENT_COUNTER_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, Arrays.asList(key));

        // Cache Miss 처리 (Lazy Loading)
        if (result == -1) {
            initCommentCountFromDB(postId);
            redisTemplate.execute(script, Arrays.asList(key));
        }

        log.debug("Comment count increased - postId: {}", postId);
    }

    // 댓글수 감소 - Lua Script 방식 (Race Condition 해결)
    @Transactional
    public void decreaseCommentCount(Long postId) {
        String key = "post:" + postId + ":commentCount";

        // Lua Script 실행 (원자적 처리) - INCRBY로 -1 처리
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(UPDATE_COUNTER_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, Arrays.asList(key), "-1");

        // Cache Miss 처리 (Lazy Loading)
        if (result == -1) {
            initCommentCountFromDB(postId);
            redisTemplate.execute(script, Arrays.asList(key), "-1");
        }

        log.debug("Comment count decreased - postId: {}", postId);
    }

    /**
     * 게시글 좋아요 토글 - Redis 방식 (Write-Back + Event Queue)
     */
    @Transactional
    public void togglePostLike(Long postId, Long userId) {
        String likeSetKey = "post:" + postId + ":likes";      // Set<userId>
        String countKey = "post:" + postId + ":likeCount";    // Counter
        String eventQueue = "post:like:events";               // Event Queue

        // Lua Script 실행 (원자적 처리)
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(TOGGLE_LIKE_SCRIPT, Long.class);
        Long result = redisTemplate.execute(
                script,
                Arrays.asList(likeSetKey, countKey),
                userId.toString()
        );

        // Cache Miss 처리 (Lazy Loading)
        if (result == -1) {
            initPostLikesFromDB(postId);
            result = redisTemplate.execute(
                    script,
                    Arrays.asList(likeSetKey, countKey),
                    userId.toString()
            );
        }

        // Event Queue에 적재 (DB 동기화용)
        String event = postId + ":" + userId + ":" + (result == 1 ? "ADD" : "REMOVE");
        stringRedisTemplate.opsForList().rightPush(eventQueue, event);

        log.debug("Post like toggled - postId: {}, userId: {}, action: {}",
                postId, userId, result == 1 ? "ADDED" : "REMOVED");

        // TODO: 알림 전송 - 트랜잭션 롤백 이슈로 인해 임시 주석 처리
        // User user = userRepository.findById(userId)
        //         .orElseThrow(() -> new CommunityException(ErrorCode.USER_NOT_FOUND));
        // CommunityPostEntity post = postRepository.findByPostIdAndIsDeletedFalse(postId)
        //         .orElseThrow(() -> new CommunityException(ErrorCode.COMMUNITY_POST_NOT_FOUND));
        // notificationSendService.sendTemplateNotificationToSingleUser(
        //         post.getAuthor().getId(),
        //         CommunityNotification.NewLike.builder()
        //                 .userName(user.getName())
        //                 .postId(post.getPostId())
        //                 .postTitle(post.getTitle())
        //                 .build());
    }

    /**
     * 게시글 좋아요 확인 - Redis 방식
     */
    public boolean isPostLiked(Long postId, Long userId) {
        String likeSetKey = "post:" + postId + ":likes";

        // Cache Miss 시 DB에서 로드
        if (Boolean.FALSE.equals(redisTemplate.hasKey(likeSetKey))) {
            initPostLikesFromDB(postId);
        }

        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(likeSetKey, userId.toString()));
    }

    /**
     * DB에서 좋아요 데이터 로드 (Lazy Loading)
     */
    private void initPostLikesFromDB(Long postId) {
        List<Long> userIds = postLikeRepository.findUserIdsByPostId(postId);

        String likeSetKey = "post:" + postId + ":likes";
        String countKey = "post:" + postId + ":likeCount";

        // Redis에 저장
        if (!userIds.isEmpty()) {
            String[] userIdStrings = userIds.stream().map(String::valueOf).toArray(String[]::new);
            redisTemplate.opsForSet().add(likeSetKey, (Object[]) userIdStrings);
        }
        redisTemplate.opsForValue().set(countKey, userIds.size());

        // TTL 설정 (7일)
        redisTemplate.expire(likeSetKey, 7, java.util.concurrent.TimeUnit.DAYS);
        redisTemplate.expire(countKey, 7, java.util.concurrent.TimeUnit.DAYS);

        log.info("Loaded post likes from DB - postId: {}, count: {}", postId, userIds.size());
    }

    /**
     * 댓글 좋아요 토글 - Redis 방식 (Write-Back + Event Queue)
     */
    @Transactional
    public void toggleCommentLike(Long commentId, Long userId) {
        String likeSetKey = "comment:" + commentId + ":likes";      // Set<userId>
        String countKey = "comment:" + commentId + ":likeCount";    // Counter
        String eventQueue = "comment:like:events";                  // Event Queue

        // Lua Script 실행 (원자적 처리)
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(TOGGLE_LIKE_SCRIPT, Long.class);
        Long result = redisTemplate.execute(
                script,
                Arrays.asList(likeSetKey, countKey),
                userId.toString()
        );

        // Cache Miss 처리 (Lazy Loading)
        if (result == -1) {
            initCommentLikesFromDB(commentId);
            result = redisTemplate.execute(
                    script,
                    Arrays.asList(likeSetKey, countKey),
                    userId.toString()
            );
        }

        // Event Queue에 적재 (DB 동기화용)
        String event = commentId + ":" + userId + ":" + (result == 1 ? "ADD" : "REMOVE");
        stringRedisTemplate.opsForList().rightPush(eventQueue, event);

        log.debug("Comment like toggled - commentId: {}, userId: {}, action: {}",
                commentId, userId, result == 1 ? "ADDED" : "REMOVED");

        // TODO: 알림 전송 - 트랜잭션 롤백 이슈로 인해 임시 주석 처리
        // User user = userRepository.findById(userId)
        //         .orElseThrow(() -> new CommunityException(ErrorCode.USER_NOT_FOUND));
        // CommunityCommentEntity comment = commentRepository.findById(commentId)
        //         .orElseThrow(() -> new CommunityException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND));
        // notificationSendService.sendTemplateNotificationToSingleUser(
        //         comment.getAuthor().getId(),
        //         CommunityNotification.NewCommentLike.builder()
        //                 .userName(user.getName())
        //                 .postId(comment.getPost().getPostId())
        //                 .postTitle(comment.getPost().getTitle())
        //                 .commentId(comment.getCommentId())
        //                 .build());
    }

    /**
     * 댓글 좋아요 확인 - Redis 방식
     */
    public boolean isCommentLiked(Long commentId, Long userId) {
        String likeSetKey = "comment:" + commentId + ":likes";

        // Cache Miss 시 DB에서 로드
        if (Boolean.FALSE.equals(redisTemplate.hasKey(likeSetKey))) {
            initCommentLikesFromDB(commentId);
        }

        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(likeSetKey, userId.toString()));
    }

    /**
     * DB에서 댓글 좋아요 데이터 로드 (Lazy Loading)
     */
    private void initCommentLikesFromDB(Long commentId) {
        List<Long> userIds = commentLikeRepository.findUserIdsByCommentId(commentId);

        String likeSetKey = "comment:" + commentId + ":likes";
        String countKey = "comment:" + commentId + ":likeCount";

        // Redis에 저장
        if (!userIds.isEmpty()) {
            String[] userIdStrings = userIds.stream().map(String::valueOf).toArray(String[]::new);
            redisTemplate.opsForSet().add(likeSetKey, (Object[]) userIdStrings);
        }
        redisTemplate.opsForValue().set(countKey, userIds.size());

        // TTL 설정 (7일)
        redisTemplate.expire(likeSetKey, 7, java.util.concurrent.TimeUnit.DAYS);
        redisTemplate.expire(countKey, 7, java.util.concurrent.TimeUnit.DAYS);

        log.info("Loaded comment likes from DB - commentId: {}, count: {}", commentId, userIds.size());
    }
}