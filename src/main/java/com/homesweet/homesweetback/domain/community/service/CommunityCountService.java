package com.homesweet.homesweetback.domain.community.service;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.community.exception.CommunityException;
import com.homesweet.homesweetback.domain.community.entity.CommunityCommentEntity;
import com.homesweet.homesweetback.domain.community.entity.CommunityCommentLikeEntity;
import com.homesweet.homesweetback.domain.community.entity.CommunityPostEntity;
import com.homesweet.homesweetback.domain.community.entity.CommunityPostLikeEntity;
import com.homesweet.homesweetback.domain.community.repository.*;
import com.homesweet.homesweetback.domain.notification.domain.notification.CommunityNotification;
import com.homesweet.homesweetback.domain.notification.service.NotificationSendService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Community Count 서비스 (카운트 테이블 분리 버전)
 * - 게시글 본문과 카운트 락 완전 분리
 * - Native SQL로 S-LOCK → X-LOCK 데드락 방지
 * - 원자적 DELETE + Unique 제약으로 Gap Lock 데드락 방지
 *
 * @author ohhalim777@gmail.com
 * @date 25. 10. 21.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityCountService {

    private final CommunityPostRepository postRepository;
    private final CommunityPostCountRepository postCountRepository;
    private final CommunityPostLikeRepository postLikeRepository;
    private final CommunityCommentRepository commentRepository;
    private final CommunityCommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;
    private final NotificationSendService notificationSendService;

    /**
     * 게시글 조회수 증가 (Native SQL - 바로 X-LOCK)
     */
    @Transactional
    public void increaseViewCount(Long postId) {
        int updated = postCountRepository.incrementViewCount(postId);
        if (updated == 0) {
            throw new CommunityException(ErrorCode.COMMUNITY_POST_NOT_FOUND);
        }
    }

    /**
     * 게시글 좋아요 토글 (원자적 DELETE + Unique 제약조건)
     *
     * 데드락 방지 전략:
     * 1. DELETE 먼저 시도 (원자적 연산 - S-LOCK 없음)
     * 2. 모든 경로에서 락 획득 순서 통일: likes → counts
     *    - 삭제 경로: likes DELETE → counts UPDATE
     *    - 추가 경로: likes INSERT → counts UPDATE
     * 3. Unique 제약 위반 시 counts는 아직 증가 전이므로 롤백 불필요
     */
    @Transactional
    public void togglePostLike(Long postId, Long userId) {
        // 먼저 삭제 시도 (원자적 연산)
        int deletedCount = postLikeRepository.deleteByPostIdAndUserId(postId, userId);

        if (deletedCount > 0) {
            // 삭제 성공 = 좋아요 취소
            postCountRepository.decrementLikeCount(postId);
        } else {
            // 삭제 실패 = 좋아요 추가
            try {
                CommunityPostEntity post = postRepository.findByPostIdAndIsDeletedFalse(postId)
                        .orElseThrow(() -> new CommunityException(ErrorCode.COMMUNITY_POST_NOT_FOUND));
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new CommunityException(ErrorCode.USER_NOT_FOUND));

                // 알림 데이터 미리 저장 (트랜잭션 커밋 후 사용)
                Long authorId = post.getAuthor().getId();
                String userName = user.getName();
                Long notifPostId = post.getPostId();
                String postTitle = post.getTitle();

                // 1️⃣ likes INSERT 먼저 (락 획득 순서 통일: likes → counts)
                CommunityPostLikeEntity newLike = CommunityPostLikeEntity.builder()
                        .post(post)
                        .user(user)
                        .build();
                postLikeRepository.save(newLike);

                // 2️⃣ counts UPDATE 나중 (데드락 방지)
                postCountRepository.incrementLikeCount(postId);

                // 트랜잭션 커밋 후 알림 전송 (락 해제 후 실행)
                if (TransactionSynchronizationManager.isSynchronizationActive()) {
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            notificationSendService.sendTemplateNotificationToSingleUser(
                                    authorId,
                                    CommunityNotification.NewLike.builder()
                                            .userName(userName)
                                            .postId(notifPostId)
                                            .postTitle(postTitle)
                                            .build());
                        }
                    });
                }
            } catch (DataIntegrityViolationException e) {
                // Unique 제약조건 위반 = 동시 요청으로 이미 추가됨
                // counts는 아직 증가시키지 않았으므로 롤백 불필요
            }
        }
    }

    /**
     * 게시글 좋아요 확인
     */
    public boolean isPostLiked(Long postId, Long userId) {
        return postLikeRepository.existsByPost_PostIdAndUser_Id(postId, userId);
    }

    /**
     * 댓글 좋아요 토글 (원자적 DELETE + Unique 제약조건)
     */
    @Transactional
    public void toggleCommentLike(Long commentId, Long userId) {
        // 먼저 삭제 시도 (원자적 연산)
        int deletedCount = commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);

        if (deletedCount > 0) {
            // 삭제 성공 = 좋아요 취소
            commentRepository.decrementLikeCount(commentId);
        } else {
            // 삭제 실패 = 좋아요 추가
            try {
                CommunityCommentEntity comment = commentRepository.findById(commentId)
                        .orElseThrow(() -> new CommunityException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND));
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new CommunityException(ErrorCode.USER_NOT_FOUND));

                // 알림 데이터 미리 저장
                Long authorId = comment.getAuthor().getId();
                String userName = user.getName();
                Long postId = comment.getPost().getPostId();
                String postTitle = comment.getPost().getTitle();

                CommunityCommentLikeEntity newLike = CommunityCommentLikeEntity.builder()
                        .comment(comment)
                        .user(user)
                        .build();
                commentLikeRepository.save(newLike);
                commentRepository.incrementLikeCount(commentId);

                // 트랜잭션 커밋 후 알림 전송
                if (TransactionSynchronizationManager.isSynchronizationActive()) {
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            notificationSendService.sendTemplateNotificationToSingleUser(
                                    authorId,
                                    CommunityNotification.NewCommentLike.builder()
                                            .userName(userName)
                                            .postId(postId)
                                            .postTitle(postTitle)
                                            .commentId(commentId)
                                            .build());
                        }
                    });
                }
            } catch (DataIntegrityViolationException e) {
                // Unique 제약조건 위반 = 동시 요청으로 이미 추가됨
            }
        }
    }

    /**
     * 댓글 좋아요 확인
     */
    public boolean isCommentLiked(Long commentId, Long userId) {
        return commentLikeRepository.existsByComment_CommentIdAndUser_Id(commentId, userId);
    }
}
