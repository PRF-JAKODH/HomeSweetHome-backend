package com.homesweet.homesweetback.domain.community.service;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.community.dto.exception.CommunityException;
import com.homesweet.homesweetback.domain.community.entity.CommunityCommentEntity;
import com.homesweet.homesweetback.domain.community.entity.CommunityCommentLikeEntity;
import com.homesweet.homesweetback.domain.community.entity.CommunityPostEntity;
import com.homesweet.homesweetback.domain.community.entity.CommunityPostLikeEntity;
import com.homesweet.homesweetback.domain.community.repository.CommunityCommentLikeRepository;
import com.homesweet.homesweetback.domain.community.repository.CommunityCommentRepository;
import com.homesweet.homesweetback.domain.community.repository.CommunityPostLikeRepository;
import com.homesweet.homesweetback.domain.community.repository.CommunityPostRepository;
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

    /**
     * 게시글 조회수 증가
     */
    @Transactional
    public void increaseViewCount(Long postId) {
        CommunityPostEntity post = postRepository.findByPostIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new CommunityException(ErrorCode.COMMUNITY_POST_NOT_FOUND));
        post.increaseViewCount();
    }

    /**
     * 게시글 좋아요 토글화
     */
    @Transactional
    public void togglePostLike(Long postId, Long userId) {
        CommunityPostEntity post = postRepository.findByPostIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new CommunityException(ErrorCode.COMMUNITY_POST_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CommunityException(ErrorCode.USER_NOT_FOUND));

        Optional<CommunityPostLikeEntity> existingLike =
                postLikeRepository.findByPostAndUser(post, user);

        if (existingLike.isPresent()) {
            postLikeRepository.delete(existingLike.get());
            post.decreaseLikeCount();
        } else {
            CommunityPostLikeEntity newLike = CommunityPostLikeEntity.builder()
                    .post(post)
                    .user(user)
                    .build();
            postLikeRepository.save(newLike);
            post.increaseLikeCount();
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
        CommunityCommentEntity comment = commentRepository.findById(commentId)
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