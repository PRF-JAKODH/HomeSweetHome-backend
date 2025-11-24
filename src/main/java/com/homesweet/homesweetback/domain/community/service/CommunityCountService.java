package com.homesweet.homesweetback.domain.community.service;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.community.exception.CommunityException;
import com.homesweet.homesweetback.domain.community.entity.CommunityCommentEntity;
import com.homesweet.homesweetback.domain.community.entity.CommunityCommentLikeEntity;
import com.homesweet.homesweetback.domain.community.entity.CommunityPostEntity;
import com.homesweet.homesweetback.domain.community.entity.CommunityPostLikeEntity;
import com.homesweet.homesweetback.domain.community.repository.CommunityCommentLikeRepository;
import com.homesweet.homesweetback.domain.community.repository.CommunityCommentRepository;
import com.homesweet.homesweetback.domain.community.repository.CommunityPostLikeRepository;
import com.homesweet.homesweetback.domain.community.repository.CommunityPostRepository;
import com.homesweet.homesweetback.domain.notification.domain.notification.CommunityNotification;
import com.homesweet.homesweetback.domain.notification.service.NotificationSendService;
import lombok.RequiredArgsConstructor;
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

    private final CommunityPostRepository postRepository;
    private final CommunityPostLikeRepository postLikeRepository;
    private final CommunityCommentRepository commentRepository;
    private final CommunityCommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;
    private final NotificationSendService notificationSendService;
    /**
     * 게시글 조회수 증가 (Atomic Query로 동시성 제어)
     * S-LOCK → X-LOCK 업그레이드 데드락 방지
     */
    @Transactional
    public void increaseViewCount(Long postId) {
        int updated = postRepository.incrementViewCount(postId);
        if (updated == 0) {
            throw new CommunityException(ErrorCode.COMMUNITY_POST_NOT_FOUND);
        }
    }

    /**
     * 게시글 좋아요 토글화 (Atomic Query + Resource Ordering)
     * 부모 테이블(posts) 먼저 UPDATE → 자식 테이블(post_likes) INSERT/DELETE
     * FK S-LOCK 데드락 방지
     */
    @Transactional
    public void togglePostLike(Long postId, Long userId) {
        // 1. 좋아요 존재 여부 확인 (락 없이 읽기만)
        boolean exists = postLikeRepository.existsByPost_PostIdAndUser_Id(postId, userId);

        if (exists) {
            // 취소: 부모 먼저 UPDATE (X-LOCK) → 자식 DELETE
            int updated = postRepository.updateLikeCount(postId, -1);
            if (updated == 0) {
                throw new CommunityException(ErrorCode.COMMUNITY_POST_NOT_FOUND);
            }
            postLikeRepository.deleteByPostIdAndUserId(postId, userId);
        } else {
            // 추가: 부모 먼저 UPDATE (X-LOCK) → 자식 INSERT
            int updated = postRepository.updateLikeCount(postId, 1);
            if (updated == 0) {
                throw new CommunityException(ErrorCode.COMMUNITY_POST_NOT_FOUND);
            }

            // 엔티티 조회 및 좋아요 생성
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CommunityException(ErrorCode.USER_NOT_FOUND));
            CommunityPostEntity post = postRepository.findByPostIdAndIsDeletedFalse(postId)
                    .orElseThrow(() -> new CommunityException(ErrorCode.COMMUNITY_POST_NOT_FOUND));

            CommunityPostLikeEntity newLike = CommunityPostLikeEntity.builder()
                    .post(post)
                    .user(user)
                    .build();
            postLikeRepository.save(newLike);

            // 알림 전송
            notificationSendService.sendTemplateNotificationToSingleUser(
                    post.getAuthor().getId(),
                    CommunityNotification.NewLike.builder()
                            .userName(user.getName())
                            .postId(post.getPostId())
                            .postTitle(post.getTitle())
                            .build());
        }
    }

    /**
     * 게시글 좋아요 확인
     */
    public boolean isPostLiked(Long postId, Long userId) {
        return postLikeRepository.existsByPost_PostIdAndUser_Id(postId, userId);
    }

    /**
     * 댓글 좋아요 토글화 (Atomic Query + Resource Ordering)
     * 부모 테이블(comments) 먼저 UPDATE → 자식 테이블(comment_likes) INSERT/DELETE
     * FK S-LOCK 데드락 방지
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
            commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);
        } else {
            // 추가: 부모 먼저 UPDATE (X-LOCK) → 자식 INSERT
            int updated = commentRepository.updateLikeCount(commentId, 1);
            if (updated == 0) {
                throw new CommunityException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND);
            }

            // 엔티티 조회 및 좋아요 생성
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CommunityException(ErrorCode.USER_NOT_FOUND));
            CommunityCommentEntity comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new CommunityException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND));

            CommunityCommentLikeEntity newLike = CommunityCommentLikeEntity.builder()
                    .comment(comment)
                    .user(user)
                    .build();
            commentLikeRepository.save(newLike);

            // 알림 전송
            notificationSendService.sendTemplateNotificationToSingleUser(
                    comment.getAuthor().getId(),
                    CommunityNotification.NewCommentLike.builder()
                            .userName(user.getName())
                            .postId(comment.getPost().getPostId())
                            .postTitle(comment.getPost().getTitle())
                            .commentId(comment.getCommentId())
                            .build());
        }
    }  

    /**
     * 댓글 좋아요 확인
     */
    public boolean isCommentLiked(Long commentId, Long userId) {
        return commentLikeRepository.existsByComment_CommentIdAndUser_Id(commentId, userId);
    }
}