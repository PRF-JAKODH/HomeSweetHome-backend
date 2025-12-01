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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Community Count 서비스
 *
 * @author ohhalim777@gmail.com
 * @date 25. 10. 21.
 */
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
    private final LikeSyncService likeSyncService;

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

    // 조회수 증가
    @Transactional
    public void increaseViewCount(Long postId) {
        String key = "post:" + postId + ":viewCount"; // redis 서버 주소

        // redis에 없으면 db에서 초기화
        Boolean wasAbsent = redisTemplate.opsForValue().setIfAbsent(key, "-1");

        // TODO 이게 뭔소리야? 문제: hasKey() + init 사이에 race condition -> 해결: SETNX 또는 Lua script로 원자적 처리
        if (Boolean.TRUE.equals(wasAbsent)) {
            initViewCountFromDB(postId);
        }

        redisCounter.incrementCounter(key);
    }

    // 댓글수 초기화
    @Transactional
    public void initCommentCountFromDB(Long postId){
        CommunityPostEntity communityPostEntity = postRepository.findByPostIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new CommunityException(ErrorCode.COMMUNITY_POST_NOT_FOUND));

        String key = "post:" + postId + ":commentCount";
        redisCounter.setCounter(key, communityPostEntity.getCommentCount());
    }

    // 댓글수 증가
    @Transactional
    public void increaseCommentCount(Long postId) {
        String key = "post:" + postId + ":commentCount";
        // redis에 없으면 db에서 초기화
        Boolean wasAbsent = redisTemplate.opsForValue().setIfAbsent(key, "-1");

        if (Boolean.TRUE.equals(wasAbsent)) {
            initCommentCountFromDB(postId);
        }

        redisCounter.incrementCounter(key);
    }

    // 댓글수 감소
    @Transactional
    public void decreaseCommentCount(Long postId) {
        String key = "post:" + postId + ":commentCount";
        // redis에 없으면 db에서 초기화
        Boolean wasAbsent = redisTemplate.opsForValue().setIfAbsent(key, "-1");

        if  (Boolean.TRUE.equals(wasAbsent)) {
            initCommentCountFromDB(postId);
        }

        redisCounter.decrementCounter(key);
    }

    /**
     * 게시글 좋아요 토글
     */
    @Transactional
    public void togglePostLike(Long postId, Long userId) {
        String key = "post:" + postId + ":likes";
        String userIdStr = String.valueOf(userId);

        if (redisCounter.isMemberOfSet(key, userIdStr)) {
            // 좋아요 제거
            redisCounter.removeFromSet(key, userIdStr);
            likeSyncService.syncPostLikeToDBAsync(postId, userId, false);  // 비동기 DB 삭제
        } else {
            // 좋아요 추가
            redisCounter.addToSet(key, userIdStr);
            likeSyncService.syncPostLikeToDBAsync(postId, userId, true);   // 비동기 DB 추가
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

    /**
     * 게시글 좋아요 확인
     */
    public boolean isPostLiked(Long postId, Long userId) {
        String key = "post:" + postId + ":likes";
        String userIdStr = String.valueOf(userId);

        return redisCounter.isMemberOfSet(key, userIdStr);
    }

    // 댓글 좋아요 토글
    @Transactional
    public void toggleCommentLike(Long commentId, Long userId) {
        String key = "comment:" + commentId + ":likes";
        String userIdStr = String.valueOf(userId);

        if (redisCounter.isMemberOfSet(key, userIdStr)) {
            // 좋아요 제거
            redisCounter.removeFromSet(key, userIdStr);
            likeSyncService.syncCommentLikeToDBAsync(commentId, userId, false);  // 비동기 DB 삭제
        } else {
            // 좋아요 추가
            redisCounter.addToSet(key, userIdStr);
            likeSyncService.syncCommentLikeToDBAsync(commentId, userId, true);   // 비동기 DB 추가
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

    /**
     * 댓글 좋아요 확인
     */
    public boolean isCommentLiked(Long commentId, Long userId) {
        String key = "comment:" + commentId + ":likes";
        String userIdStr = String.valueOf(userId);

        return redisCounter.isMemberOfSet(key, userIdStr);
    }
}