package com.homesweet.homesweetback.domain.community.service;

import com.homesweet.homesweetback.domain.community.entity.CommunityPostEntity;
import com.homesweet.homesweetback.domain.community.repository.CommunityPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class Scheduler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CommunityPostRepository communityPostRepository;

    @Transactional
    @Scheduled(initialDelay = 1000000, fixedDelay = 1000000)
    public void updateCountData() {
        //  Redis에서 모든 조회수 key 찾기
        Set<String> keys = redisTemplate.keys("post:*:viewCount"); // * 모든 문자들 매칭

        //  각 key마다 반복
        for (String key : keys) {

            //  key에서 postId 추출
            String[] parts = key.split(":");
            Long postId = Long.parseLong(parts[1]);

            // 키에 대한 밸류 가져옴 redis에서
            Integer viewCount = (Integer) redisTemplate.opsForValue().get(key);
            if (viewCount == null) continue;

            // db저장
            if (communityPostRepository.findByPostIdAndIsDeletedFalse(postId).isPresent()) {
                communityPostRepository.updateViewCount(postId, viewCount);
            }
            // redis 삭제
            redisTemplate.delete(key);
        }
    }

    // 댓글 수정
    @Transactional
    @Scheduled(initialDelay = 1500000, fixedDelay = 1500000)
    public void updateCommentData() {
            //  Redis에서 모든 조회수 key 찾기
        Set<String> keys = redisTemplate.keys("post:*:commentCount");

        for (String key : keys) {
            String[] parts = key.split(":");
            Long postId = Long.parseLong(parts[1]);
            Integer commentCount = (Integer) redisTemplate.opsForValue().get(key);
            if (commentCount == null) continue;

            if (communityPostRepository.findByPostIdAndIsDeletedFalse(postId).isPresent()) {
                communityPostRepository.setCommentCount(postId, commentCount); // setcomment 증가 감소
            }
            redisTemplate.delete(key);
        }


    }
}