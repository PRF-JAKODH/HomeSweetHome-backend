package com.homesweet.homesweetback.domain.community.scheduler;

import com.homesweet.homesweetback.domain.community.repository.CommunityPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class Scheduler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CommunityPostRepository communityPostRepository;

    @Transactional
    @Scheduled(initialDelay = 1000000, fixedDelay = 1000000)
    public void updateCountData() {
        //  scan으로 변경
        ScanOptions options = ScanOptions.scanOptions()
                .match("post:*:viewCount")
                .count(100)
                .build();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {

            //  각 key마다 반복
            while(cursor.hasNext()) {
                String key = cursor.next();

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
    }

    // 댓글 수정
    @Transactional
    @Scheduled(initialDelay = 1500000, fixedDelay = 1500000)
    public void updateCommentData() {
            //  Redis에서 모든 조회수 key 찾기
        ScanOptions options =ScanOptions.scanOptions()
                .match("post:*:commentCount")
                .count(100)
                .build();

            try (Cursor<String> cursor = redisTemplate.scan(options)) {

                while (cursor.hasNext()){
                    String key = cursor.next();

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
}