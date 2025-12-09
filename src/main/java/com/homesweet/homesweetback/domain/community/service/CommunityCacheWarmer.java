package com.homesweet.homesweetback.domain.community.service;

import com.homesweet.homesweetback.domain.community.entity.CommunityPostEntity;
import com.homesweet.homesweetback.domain.community.repository.CommunityPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 커뮤니티 캐시 워밍업
 * 인기 게시글을 주기적으로 Redis에 사전 로드하여 캐시 히트율 향상
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityCacheWarmer {

    private final CommunityPostRepository postRepository;
    private final CommunityPostService postService;

    /**
     * 인기 게시글 캐시 워밍업 (5분마다 실행)
     * 조회수 상위 100개 게시글을 Redis에 사전 로드
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000) // 5분마다, 1분 후 시작
    public void warmupHotPosts() {
        try {
            log.info("Starting cache warmup for hot posts...");
            
            // 조회수 상위 100개 게시글 조회
            Pageable pageable = PageRequest.of(0, 100);
            List<CommunityPostEntity> hotPosts = postRepository
                    .findTop100ByIsDeletedFalseOrderByViewCountDesc();
            
            int cachedCount = 0;
            for (CommunityPostEntity post : hotPosts) {
                try {
                    // getPost() 호출 시 자동으로 캐싱됨
                    postService.getPost(post.getPostId());
                    cachedCount++;
                } catch (Exception e) {
                    log.warn("Failed to cache post: {}", post.getPostId(), e);
                }
            }
            
            log.info("Cache warmup completed: {} posts cached", cachedCount);
        } catch (Exception e) {
            log.error("Cache warmup failed", e);
        }
    }

    /**
     * 최신 게시글 캐시 워밍업 (10분마다 실행)
     * 최신 50개 게시글을 Redis에 사전 로드
     */
    @Scheduled(fixedDelay = 600000, initialDelay = 120000) // 10분마다, 2분 후 시작
    public void warmupRecentPosts() {
        try {
            log.info("Starting cache warmup for recent posts...");
            
            // 최신 50개 게시글 조회
            Pageable pageable = PageRequest.of(0, 50);
            List<CommunityPostEntity> recentPosts = postRepository
                    .findTop50ByIsDeletedFalseOrderByCreatedAtDesc();
            
            int cachedCount = 0;
            for (CommunityPostEntity post : recentPosts) {
                try {
                    postService.getPost(post.getPostId());
                    cachedCount++;
                } catch (Exception e) {
                    log.warn("Failed to cache post: {}", post.getPostId(), e);
                }
            }
            
            log.info("Recent posts cache warmup completed: {} posts cached", cachedCount);
        } catch (Exception e) {
            log.error("Recent posts cache warmup failed", e);
        }
    }
}
