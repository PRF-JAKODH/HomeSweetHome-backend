package com.homesweet.homesweetback.domain.community.service;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.community.dto.CommunityCommentRequest;
import com.homesweet.homesweetback.domain.community.dto.CommunityCommentResponse;
import com.homesweet.homesweetback.domain.community.dto.exception.CommunityException;
import com.homesweet.homesweetback.domain.community.entity.CommunityCommentEntity;
import com.homesweet.homesweetback.domain.community.entity.CommunityPostEntity;
import com.homesweet.homesweetback.domain.community.repository.CommunityCommentRepository;
import com.homesweet.homesweetback.domain.community.repository.CommunityPostRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityCommentService {
    private final CommunityCommentRepository commentRepository;
    private final CommunityPostRepository postRepository;
    private final UserRepository userRepository;

    /**
     * 댓글 작성
     */
    @Transactional
    // 사용자 검증
    public CommunityCommentResponse createComment(Long postId, CommunityCommentRequest request, Long userId) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new CommunityException(ErrorCode.USER_NOT_FOUND));

        // 게시글 존재 여부 확인
        CommunityPostEntity post = postRepository.findByPostIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new CommunityException(ErrorCode.COMMUNITY_POST_NOT_FOUND));

        // 대댓글인 경우 부모 댓글 존재 여부 확인
        if (request.parentCommentId() != null) {
            commentRepository.findById(request.parentCommentId())
                    .orElseThrow(() -> new CommunityException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND));
        }

        // 댓글 엔티티 생성 및 연결
        CommunityCommentEntity comment = CommunityCommentEntity.builder()
                .post(post)
                .author(author)
                .content(request.content())
                .parentCommentId((request.parentCommentId()))
                .build();

        CommunityCommentEntity savedComment = commentRepository.save(comment);

        // 게시글의 댓글 수 증가
        post.increaseCommentCount();

        return CommunityCommentResponse.from(savedComment);
    }

    /**
     * 해당 게시글의 모든 댓글 조회
     */
    public List<CommunityCommentResponse> getCommentsByPostId(Long postId) {
        List<CommunityCommentEntity> comments =
                commentRepository.findByPost_PostIdAndIsDeletedFalse(postId);
        return comments.stream()
                .map(CommunityCommentResponse::from)
                .toList();
    }

    /**
     * 댓글 수정
     */
    @Transactional
    public CommunityCommentResponse updateComment(Long commentId, CommunityCommentRequest request, Long userId) {
        // 댓글 조회
        CommunityCommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommunityException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND));

        // 작성자 본인 확인
        if (!comment.isAuthor(userId)) {
            throw new CommunityException(ErrorCode.COMMUNITY_COMMENT_FORBIDDEN);
        }

        // 댓글 수정
        comment.updateComment(request.content());

        return CommunityCommentResponse.from(comment);
    }

    /**
     * 댓글 삭제
     */
    @Transactional
    public void deleteComment(Long commentId, Long postId, Long userId) {
        // 댓글 조회
        CommunityCommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommunityException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND));

        // 작성자 본인 확인
        if (!comment.isAuthor(userId)) {
            throw new CommunityException(ErrorCode.COMMUNITY_COMMENT_FORBIDDEN);
        }

        // 게시글 조회 및 댓글 수 감소
        CommunityPostEntity post = postRepository.findByPostIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new CommunityException(ErrorCode.COMMUNITY_POST_NOT_FOUND));
        post.decreaseCommentCount();

        // 댓글 소프트 삭제
        comment.deleteComment();
    }
}
