# 커뮤니티 캐싱 최적화 완료 보고서

## 📊 성능 개선 결과 요약

| 지표 | 최적화 전 | 최적화 후 | 개선 |
|------|----------|----------|------|
| 게시글 상세 p(95) | 1398ms | 698ms | **-50%** |
| 게시글 목록 p(95) | 1761ms | 837ms | **-52%** |
| RPS (VU 100) | 39.5 | 47.1 | **+19%** |
| 에러율 | 0% | 0% | ✅ |

---

## ✅ 구현된 기능

### 1. Redis 캐싱 (Cache-Aside 패턴)

| 항목 | 캐시 키 | TTL | 파일 |
|------|--------|-----|------|
| 게시글 상세 | `communityPost::postId` | 1시간 | `CommunityPostService.getPost()` |
| 게시글 목록 | `communityPostList::page:size` | 1분 | `CommunityPostService.getPosts()` |
| 댓글 목록 | `comments::post::postId` | 30분 | `CommunityCommentService.getCommentsByPostId()` |

### 2. 캐시 무효화

- `updatePost()`, `deletePost()` → 게시글 캐시 삭제
- `createComment()`, `updateComment()`, `deleteComment()` → 댓글 캐시 삭제

### 3. 캐시 워밍업 스케줄러

- `CommunityScheduler.warmupPopularPostsCache()`
- 서버 시작 5초 후 + 1시간마다 실행
- 최근 게시글 100개의 좋아요/조회수 미리 로딩

### 4. N+1 쿼리 최적화

- `CommunityPostRepository.findByPostIdAndIsDeletedFalse` → `@EntityGraph` 추가
- `CommunityCommentRepository.findByPost_PostIdAndIsDeletedFalse` → `@EntityGraph` 추가

---

## 📁 수정된 파일 목록

```
src/main/java/.../community/
├── service/
│   ├── CommunityPostService.java      # 게시글 캐싱 로직
│   ├── CommunityCommentService.java   # 댓글 캐싱 로직
│   └── CommunityCountService.java     # 카운터 캐싱 (기존)
├── repository/
│   ├── CommunityPostRepository.java   # @EntityGraph 추가
│   └── CommunityCommentRepository.java # @EntityGraph 추가
└── scheduler/
    └── CommunityScheduler.java        # 워밍업 스케줄러 추가
```

---

## 🎯 다음 단계: Kafka + CQRS

### 현재 아키텍처
```
[클라이언트] → [Backend] → [Redis + MySQL 동기] → [응답]
```

### 목표 아키텍처 (Kafka 도입)
```
[클라이언트] → [Backend] → [Redis만 동기] → [즉시 응답]
                              ↓
                        [Kafka Producer]
                              ↓
                        [Kafka Consumer] → [MySQL 비동기]
```

### 구현 대상
1. **좋아요 토글** → Kafka 이벤트 발행
2. **조회수 증가** → Kafka 이벤트 발행
3. **댓글 작성** → Kafka 이벤트 발행 (선택)

### 필요한 Kafka 토픽
- `community.like.events` - 좋아요 이벤트
- `community.view.events` - 조회수 이벤트
- `community.comment.events` - 댓글 이벤트 (선택)

### 예상 효과
- 좋아요 응답: 600ms → **50ms**
- 조회수 증가: 150ms → **20ms**

---

## 🔧 인프라 정보

| 컴포넌트 | 스펙 | 한계 |
|----------|------|------|
| MySQL | t3a.small (2vCPU, 2GB) | ~100 RPS |
| Redis | r6g.medium (2vCPU, 8GB) | ~50,000 RPS |
| Backend | t3.medium (2vCPU, 4GB) | ~200 RPS |

**현재 안정 VU: 100** (MySQL 병목)

---

## 📝 참고 코드

### 캐싱 로직 예시 (getPost)
```java
public CommunityPostResponse getPost(Long postId) {
    String cacheKey = POST_CACHE_PREFIX + postId;
    
    // 1. 캐시 조회
    String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);
    if (cachedJson != null) {
        CommunityPostResponse cached = objectMapper.readValue(cachedJson, CommunityPostResponse.class);
        // 카운터는 항상 최신값 조회
        return new CommunityPostResponse(..., 
            communityCountService.getViewCountFromCache(postId),
            communityCountService.getLikeCountFromCache(postId),
            ...);
    }
    
    // 2. Cache Miss - DB 조회
    CommunityPostEntity post = postRepository.findByPostIdAndIsDeletedFalse(postId);
    
    // 3. 캐시 저장
    stringRedisTemplate.opsForValue().set(cacheKey, json, POST_CACHE_TTL);
    
    return response;
}
```

### k6 테스트 명령어
```bash
k6 run -e BASE_URL=http://3.34.138.75:8080 \
  -e MIN_POST_ID=1 \
  -e MAX_POST_ID=205 \
  k6/community/multiUserCommunity.js
```

---

*작성일: 2025-12-08*
