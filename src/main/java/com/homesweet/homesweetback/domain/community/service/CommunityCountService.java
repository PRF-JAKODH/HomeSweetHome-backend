package com.homesweet.homesweetback.domain.community.service;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.community.entity.CommunityPostEntity;
import com.homesweet.homesweetback.domain.community.exception.CommunityException;
import com.homesweet.homesweetback.domain.community.repository.CommunityCommentLikeRepository;
import com.homesweet.homesweetback.domain.community.repository.CommunityCommentRepository;
import com.homesweet.homesweetback.domain.community.repository.CommunityPostLikeRepository;
import com.homesweet.homesweetback.domain.community.repository.CommunityPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 조회용
public class CommunityCountService {

    private final RedisCounter redisCounter;
    private final CommunityPostRepository postRepository;
    private final CommunityPostLikeRepository postLikeRepository;
    private final CommunityCommentRepository commentRepository;
    private final CommunityCommentLikeRepository commentLikeRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    // 상수 정의 (Key 분리)
    private static final String POST_LIKE_KEY_PREFIX = "post:%d:likes";
    private static final String POST_LIKE_EVENT_QUEUE = "post:like:events"; // 게시글 좋아요 대기열

    private static final String COMMENT_LIKE_KEY_PREFIX = "comment:%d:likes";
    private static final String COMMENT_LIKE_EVENT_QUEUE = "comment:like:events"; // 댓글 좋아요 대기열


    @Transactional
    public void initViewCountFromDB(Long postId){
        CommunityPostEntity communityPostEntity = postRepository.findByPostIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new CommunityException(ErrorCode.COMMUNITY_POST_NOT_FOUND));

        String key = "post:" + postId + ":viewCount";
        redisCounter.setCounter(key, communityPostEntity.getViewCount());
    }

    @Transactional
    public void increaseViewCount(Long postId) {
        String key = "post:" + postId + ":viewCount";
        Boolean wasAbsent = redisTemplate.opsForValue().setIfAbsent(key, -1);

        if (Boolean.TRUE.equals(wasAbsent)) {
            initViewCountFromDB(postId);
        }
        redisCounter.incrementCounter(key);
    }

    @Transactional
    public void initCommentCountFromDB(Long postId){
        CommunityPostEntity communityPostEntity = postRepository.findByPostIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new CommunityException(ErrorCode.COMMUNITY_POST_NOT_FOUND));

        String key = "post:" + postId + ":commentCount";
        redisCounter.setCounter(key, communityPostEntity.getCommentCount());
    }

    @Transactional
    public void increaseCommentCount(Long postId) {
        String key = "post:" + postId + ":commentCount";
        Boolean wasAbsent = redisTemplate.opsForValue().setIfAbsent(key, -1);

        if (Boolean.TRUE.equals(wasAbsent)) {
            initCommentCountFromDB(postId);
        }
        redisCounter.incrementCounter(key);
    }

    @Transactional
    public void decreaseCommentCount(Long postId) {
        String key = "post:" + postId + ":commentCount";
        Boolean wasAbsent = redisTemplate.opsForValue().setIfAbsent(key, -1);

        if (Boolean.TRUE.equals(wasAbsent)) {
            initCommentCountFromDB(postId);
        }
        redisCounter.decrementCounter(key);
    }

    /**
     * Lazy Loading: 캐시에 없으면 DB에서 로딩
     */
    private void ensurePostLikesLoaded(Long postId) {
        String key = String.format(POST_LIKE_KEY_PREFIX, postId);

        if (!redisCounter.hasKey(key)) {
            log.info("Cache Miss: Loading likes for post {}", postId);
            // DB 조회 (인덱스 타므로 빠름)
            List<Long> userIds = postLikeRepository.findAllUserIdsByPostId(postId);

            if (!userIds.isEmpty()) {
                Set<String> userIdStrs = userIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.toSet());
                redisCounter.addAllToSet(key, userIdStrs);
                redisCounter.expire(key, 3, TimeUnit.HOURS); // 3시간 후 만료
            }
        } else {
            redisCounter.expire(key, 3, TimeUnit.HOURS); // 접근 시 만료시간 연장
        }
    }

    /**
     * 게시글 좋아요 토글
     * ★ 중요: DB 커넥션을 쓰지 않기 위해 @Transactional 제거
     */
    public void togglePostLike(Long postId, Long userId) {
        // 1. 캐시 확보 (없으면 로딩)
        ensurePostLikesLoaded(postId);

        String key = String.format(POST_LIKE_KEY_PREFIX, postId);
        String userIdStr = String.valueOf(userId);

        // 2. Redis Set 토글
        boolean isLiked = redisCounter.isMemberOfSet(key, userIdStr);

        if (isLiked) {
            redisCounter.removeFromSet(key, userIdStr);
            // 3. 큐에 "삭제" 이벤트 적재 (Write Back)
            redisCounter.pushToQueue(POST_LIKE_EVENT_QUEUE, "REM:" + postId + ":" + userId);
        } else {
            redisCounter.addToSet(key, userIdStr);
            // 3. 큐에 "추가" 이벤트 적재 (Write Back)
            redisCounter.pushToQueue(POST_LIKE_EVENT_QUEUE, "ADD:" + postId + ":" + userId);
        }

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

    public boolean isPostLiked(Long postId, Long userId) {
        ensurePostLikesLoaded(postId);
        String key = String.format(POST_LIKE_KEY_PREFIX, postId);
        return redisCounter.isMemberOfSet(key, String.valueOf(userId));
    }

    public Long getPostLikeCount(Long postId) {
        ensurePostLikesLoaded(postId);
        String key = String.format(POST_LIKE_KEY_PREFIX, postId);
        return redisCounter.getSetSize(key);
    }


    // ================= [댓글 좋아요 로직 (변경됨)] =================

    private void ensureCommentLikesLoaded(Long commentId) {
        String key = String.format(COMMENT_LIKE_KEY_PREFIX, commentId);

        if (!redisCounter.hasKey(key)) {
            log.info("Cache Miss: Loading likes for comment {}", commentId);
            // DB 조회
            List<Long> userIds = commentLikeRepository.findAllUserIdsByCommentId(commentId);

            if (!userIds.isEmpty()) {
                Set<String> userIdStrs = userIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.toSet());
                redisCounter.addAllToSet(key, userIdStrs);
                redisCounter.expire(key, 3, TimeUnit.HOURS);
            }
        } else {
            redisCounter.expire(key, 3, TimeUnit.HOURS);
        }
    }

    /**
     * 댓글 좋아요 토글
     * ★ 중요: @Transactional 제거
     */
    public void toggleCommentLike(Long commentId, Long userId) {
        ensureCommentLikesLoaded(commentId);

        String key = String.format(COMMENT_LIKE_KEY_PREFIX, commentId);
        String userIdStr = String.valueOf(userId);

        boolean isLiked = redisCounter.isMemberOfSet(key, userIdStr);

        if (isLiked) {
            redisCounter.removeFromSet(key, userIdStr);
            redisCounter.pushToQueue(COMMENT_LIKE_EVENT_QUEUE, "REM:" + commentId + ":" + userId);
        } else {
            redisCounter.addToSet(key, userIdStr);
            redisCounter.pushToQueue(COMMENT_LIKE_EVENT_QUEUE, "ADD:" + commentId + ":" + userId);
        }

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

    public boolean isCommentLiked(Long commentId, Long userId) {
        ensureCommentLikesLoaded(commentId); // Post -> Comment로 수정됨
        String key = String.format(COMMENT_LIKE_KEY_PREFIX, commentId);
        return redisCounter.isMemberOfSet(key, String.valueOf(userId));
    }

    public Long getCommentLikeCount(Long commentId) {
        ensureCommentLikesLoaded(commentId); // Post -> Comment로 수정됨
        String key = String.format(COMMENT_LIKE_KEY_PREFIX, commentId);
        return redisCounter.getSetSize(key);
    }
}