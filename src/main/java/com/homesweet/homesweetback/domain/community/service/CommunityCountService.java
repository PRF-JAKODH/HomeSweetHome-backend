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
     *
     */
    @Transactional
    public void togglePostLike(Long postId, Long userId) {
        // 1. 좋아요 존재 여부 확인
        boolean exists = postLikeRepository.existsByPost_PostIdAndUser_Id(postId, userId);

        if (exists) {
            // 취소: 부모 먼저 UPDATE (X-LOCK) → 자식 DELETE
            int updated = postRepository.updateLikeCount(postId, -1);
            if (updated == 0) {
                throw new CommunityException(ErrorCode.COMMUNITY_POST_NOT_FOUND);
            }
            int deleted = postLikeRepository.deleteByPostIdAndUserId(postId, userId);
            if (deleted == 0) {
                // 이미 다른 트랜잭션이 삭제함 → 카운트 원복
                postRepository.updateLikeCount(postId, 1);
            }
        } else {
            // 추가: 부모 먼저 UPDATE (X-LOCK) → 자식 INSERT
            int updated = postRepository.updateLikeCount(postId, 1);
            if (updated == 0) {
                throw new CommunityException(ErrorCode.COMMUNITY_POST_NOT_FOUND);
            }

            // 네이티브 INSERT IGNORE (중복 시 에러 없이 0 반환)
            int inserted = postLikeRepository.insertPostLike(postId, userId);
            if (inserted == 0) {
                // 이미 다른 트랜잭션이 추가함 → 카운트 원복
                postRepository.updateLikeCount(postId, -1);
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
    }

    /**
     * 게시글 좋아요 확인
     */
    public boolean isPostLiked(Long postId, Long userId) {
        return postLikeRepository.existsByPost_PostIdAndUser_Id(postId, userId);
    }

    /**
     *
     */
    @Transactional
    public void toggleCommentLike(Long commentId, Long userId) {
        // 1. 좋아요 존재 여부 확인 (락 없이 읽기만)
        boolean exists = commentLikeRepository.existsByComment_CommentIdAndUser_Id(commentId, userId);

        if (exists) {
            // 취소: 부모 먼저 UPDATE (X-LOCK) → 자식 DELETE
            int updated = commentRepository.updateLikeCount(commentId, -1);
            if (updated == 0) {
                throw new CommunityException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND);
            }
            int deleted = commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);
            if (deleted == 0) {
                // 이미 다른 트랜잭션이 삭제함 → 카운트 원복
                commentRepository.updateLikeCount(commentId, 1);
            }
        } else {
            // 추가: 부모 먼저 UPDATE (X-LOCK) → 자식 INSERT
            int updated = commentRepository.updateLikeCount(commentId, 1);
            if (updated == 0) {
                throw new CommunityException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND);
            }

            // 네이티브 INSERT IGNORE (중복 시 에러 없이 0 반환)
            int inserted = commentLikeRepository.insertCommentLike(commentId, userId);
            if (inserted == 0) {
                // 이미 다른 트랜잭션이 추가함 → 카운트 원복
                commentRepository.updateLikeCount(commentId, -1);
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
    }

    /**
     * 댓글 좋아요 확인
     */
    public boolean isCommentLiked(Long commentId, Long userId) {
        return commentLikeRepository.existsByComment_CommentIdAndUser_Id(commentId, userId);
    }
}