package com.homesweet.homesweetback.domain.community.service;

import com.homesweet.homesweetback.domain.community.repository.CommunityCommentLikeRepository;
import com.homesweet.homesweetback.domain.community.repository.CommunityPostLikeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class LikeSyncService {

    private final CommunityPostLikeRepository postLikeRepository;
    private final CommunityCommentLikeRepository commentLikeRepository;

    @Async("likeTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompletableFuture<Void> syncPostLikeToDBAsync(Long postId, Long userId, boolean isAdd) {
        try {
            if (isAdd) {
                postLikeRepository.insertPostLike(postId, userId);
            } else {
                postLikeRepository.deleteByPostIdAndUserId(postId, userId);
            }
        } catch (Exception e) {
            log.error("좋아요 동기화 실패: postId={}, userId={}", postId, userId, e);
            // 실패해도 Redis는 이미 반영됨 (eventually consistent)
        }
        return CompletableFuture.completedFuture(null);
    }

    @Async("likeTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompletableFuture<Void> syncCommentLikeToDBAsync(Long commentId, Long userId, boolean isAdd) {
        try {
            if (isAdd) {
                commentLikeRepository.insertCommentLike(commentId, userId);
            } else {
                commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);
            }
        } catch (Exception e) {
            log.error("댓글 좋아요 동기화 실패: commentId={}, userId={}", commentId, userId, e);
        }
        return CompletableFuture.completedFuture(null);
    }
}