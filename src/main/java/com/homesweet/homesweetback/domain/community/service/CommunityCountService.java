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

import java.util.Optional;

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
    private final RedisCommunityCountService redisCountService;

    /**
     * 게시글 조회수 증가 (Redis 사용 - 초고성능)
     * - Redis의 원자적 연산으로 데드락 없음
     * - 주기적으로 DB에 동기화 (배치 작업)
     */
    public void increaseViewCount(Long postId) {
        // 게시글 존재 여부만 확인 (조회 없이)
        if (!postRepository.existsById(postId)) {
            throw new CommunityException(ErrorCode.COMMUNITY_POST_NOT_FOUND);
        }
        // Redis에만 증가 (초고속, 데드락 없음)
        redisCountService.incrementViewCount(postId);
    }

    /**
     * 게시글 좋아요 토글 (벌크 업데이트 - 데드락 방지)
     * - Entity 조회 없이 직접 UPDATE 쿼리 실행
     * - 좋아요 레코드만 DB에서 관리
     */
    @Transactional
    public void togglePostLike(Long postId, Long userId) {
        // 게시글 존재 확인
        CommunityPostEntity post = postRepository.findByPostIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new CommunityException(ErrorCode.COMMUNITY_POST_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CommunityException(ErrorCode.USER_NOT_FOUND));

        Optional<CommunityPostLikeEntity> existingLike =
                postLikeRepository.findByPostAndUser(post, user);

        if (existingLike.isPresent()) {
            // 좋아요 취소
            postLikeRepository.delete(existingLike.get());
            // 벌크 업데이트 (데드락 없음)
            postRepository.updateLikeCount(postId, -1);
        } else {
            // 좋아요 추가
            CommunityPostLikeEntity newLike = CommunityPostLikeEntity.builder()
                    .post(post)
                    .user(user)
                    .build();
            postLikeRepository.save(newLike);
            // 벌크 업데이트 (데드락 없음)
            postRepository.updateLikeCount(postId, 1);
        }
    }

    /**
     * 게시글 좋아요 확인
     */
    public boolean isPostLiked(Long postId, Long userId) {
        return postLikeRepository.existsByPost_PostIdAndUser_Id(postId, userId);
    }

    /**
     * 댓글 좋아요 토글화
     */
    @Transactional
    public void toggleCommentLike(Long commentId, Long userId) {
        CommunityCommentEntity comment = commentRepository.findByIdWithPessimisticLock(commentId)
                .orElseThrow(() -> new CommunityException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CommunityException(ErrorCode.USER_NOT_FOUND));

        Optional<CommunityCommentLikeEntity> existingLike =
                commentLikeRepository.findByCommentAndUser(comment, user);

        if (existingLike.isPresent()) {
            commentLikeRepository.delete(existingLike.get());
            comment.decreaseLikeCount();
        } else {
            CommunityCommentLikeEntity newLike = CommunityCommentLikeEntity.builder()
                    .comment(comment)
                    .user(user)
                    .build();
            commentLikeRepository.save(newLike);
            comment.increaseLikeCount();
        }


    }  

    /**
     * 댓글 좋아요 확인
     */
    public boolean isCommentLiked(Long commentId, Long userId) {
        return commentLikeRepository.existsByComment_CommentIdAndUser_Id(commentId, userId);
    }
}