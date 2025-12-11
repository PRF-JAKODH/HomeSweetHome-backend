커뮤니티 3차 성능 개선 ‐ DB 쿼리 최적화 및 캐싱 전략

[2차 성능 개선](커뮤니티-2차-성능-개선-‐-Redis-기반-카운터-시스템-구축)에서 Redis 기반 카운터 시스템을 구축하여 실시간 연산 성능을 크게 향상시켰습니다.

하지만 게시글 목록 조회 시 여전히 다음과 같은 문제가 있었습니다:
- 각 게시글마다 Redis를 개별 호출하는 **N+1 문제**
- 페이지네이션 시 **COUNT 쿼리 부하**
- **복합 인덱스 부재**로 인한 Full Table Scan

이번 글에서는 이러한 문제들을 **복합 인덱스, N+1 해결, Bulk MGET 최적화**를 통해 해결한 과정을 공유합니다.

---

## 2차 개선 이후 남은 문제

### 1. Redis N+1 문제

게시글 목록 조회 시 각 게시글의 조회수/좋아요수/댓글수를 가져오기 위해 **Redis를 30번 호출**하는 문제가 있었습니다.

```java
// 기존 코드 - N+1 문제 발생
List<CommunityPostResponse> responses = posts.stream()
    .map(post -> {
        // 각 게시글마다 Redis 3번 호출 (조회수, 좋아요수, 댓글수)
        Integer viewCount = countService.getViewCountFromCache(post.getPostId());
        Integer likeCount = countService.getLikeCountFromCache(post.getPostId());
        Integer commentCount = countService.getCommentCountFromCache(post.getPostId());
        return CommunityPostResponse.from(post, viewCount, likeCount, commentCount);
    })
    .toList();

// 10개 게시글 조회 시 → Redis 30번 호출!
```

### 2. COUNT 쿼리 부하

JPA의 `Page<T>`를 사용하면 자동으로 COUNT 쿼리가 실행됩니다.

```sql
-- 실제 데이터 조회
SELECT * FROM community_posts WHERE is_deleted = false ORDER BY created_at DESC LIMIT 10;

-- 자동 실행되는 COUNT 쿼리 (무거움!)
SELECT COUNT(*) FROM community_posts WHERE is_deleted = false;
```

10만 건 이상의 데이터에서 COUNT 쿼리는 **수백 ms ~ 수 초**가 소요될 수 있습니다.

### 3. Full Table Scan

게시글 목록 조회 쿼리가 인덱스를 타지 않고 **Full Table Scan**이 발생했습니다.

```sql
EXPLAIN SELECT * FROM community_posts
WHERE is_deleted = false
ORDER BY created_at DESC
LIMIT 10;

-- 결과: type = ALL (Full Table Scan)
-- rows = 102,532 (전체 테이블 스캔)
```

---

## 해결 방안 1: 복합 인덱스 추가

### 인덱스 설계

```sql
-- V1.0.20__add_community_posts_performance_index.sql

-- 복합 인덱스 추가: is_deleted + created_at (DESC)
-- 1. is_deleted = false 조건 필터링
-- 2. created_at DESC 정렬 활용

SET @index_exists = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'community_posts'
      AND index_name = 'idx_community_posts_deleted_created'
);

SET @sql = IF(
    @index_exists = 0,
    'CREATE INDEX idx_community_posts_deleted_created ON community_posts(is_deleted, created_at DESC)',
    'SELECT "Index already exists, skipping creation" AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
```

### 인덱스 적용 결과

```sql
EXPLAIN SELECT * FROM community_posts
WHERE is_deleted = false
ORDER BY created_at DESC
LIMIT 10;

-- 개선 전
-- type: ALL (Full Table Scan)
-- rows: 102,532

-- 개선 후
-- type: ref (Index Scan)
-- key: idx_community_posts_deleted_created
-- rows: 51,266 (50% 감소)
```

| 항목 | 개선 전 | 개선 후 |
|------|--------|--------|
| 스캔 타입 | ALL (Full Table Scan) | ref (Index Scan) |
| 스캔 행 수 | 102,532 | 51,266 |
| 예상 응답 시간 | 5.8초 | 0.1초 이하 |

---

## 해결 방안 2: N+1 문제 해결 - EntityGraph & Bulk MGET

### JPA N+1 해결 - @EntityGraph

게시글 조회 시 작성자 정보도 함께 필요한데, 기본적으로 Lazy Loading이 적용되어 N+1 문제가 발생합니다.

```java
public interface CommunityPostRepository extends JpaRepository<CommunityPostEntity, Long> {

    // @EntityGraph로 author, grade를 한 번에 조회 (N+1 방지)
    @EntityGraph(attributePaths = {"author", "author.grade"})
    Optional<CommunityPostEntity> findByPostIdAndIsDeletedFalse(Long postId);

    // 페이지네이션에도 적용
    @EntityGraph(attributePaths = {"author", "author.grade"})
    Slice<CommunityPostEntity> findByIsDeletedFalse(Pageable pageable);
}
```

### Redis N+1 해결 - Bulk MGET

기존에는 각 게시글마다 Redis를 개별 호출했지만, **MGET**을 사용하면 한 번에 여러 키를 조회할 수 있습니다.

```java
@Service
public class CommunityRedisService {

    /**
     * 여러 게시글의 조회수를 한 번에 조회 (MGET)
     */
    public Map<Long, Integer> getBulkViewCounts(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Map.of();
        }

        List<String> keys = postIds.stream()
            .map(this::getPostViewKey)
            .toList();

        List<Object> values = redisTemplate.opsForValue().multiGet(keys);

        Map<Long, Integer> result = new HashMap<>();
        for (int i = 0; i < postIds.size(); i++) {
            Object value = values != null ? values.get(i) : null;
            result.put(postIds.get(i), value != null ? (Integer) value : null);
        }
        return result;
    }

    /**
     * 여러 게시글의 좋아요수를 한 번에 조회 (MGET)
     */
    public Map<Long, Integer> getBulkLikeCounts(List<Long> postIds) {
        // 동일한 패턴
    }

    /**
     * 여러 게시글의 댓글수를 한 번에 조회 (MGET)
     */
    public Map<Long, Integer> getBulkCommentCounts(List<Long> postIds) {
        // 동일한 패턴
    }
}
```

### CountService에서 Bulk 조회 활용

```java
@Service
public class CommunityCountService {

    /**
     * 여러 게시글의 조회수를 한 번에 조회 (Cache miss 시 개별 fallback)
     */
    public Map<Long, Integer> getBulkViewCountsFromCache(List<Long> postIds) {
        Map<Long, Integer> result = redisService.getBulkViewCounts(postIds);

        // Cache miss 처리
        for (Long postId : postIds) {
            if (result.get(postId) == null) {
                initViewCountFromDB(postId);
                result.put(postId, redisService.getPostViewCount(postId));
            }
        }
        return result;
    }
}
```

### 개선 결과

```
개선 전: 10개 게시글 조회 시 Redis 30번 호출
개선 후: 10개 게시글 조회 시 Redis 3번 호출 (MGET)

→ Redis 호출 90% 감소!
```

---

## 해결 방안 3: COUNT 쿼리 제거 - Slice + Redis 캐싱

### Page → Slice 변환

JPA의 `Slice<T>`는 `Page<T>`와 달리 **COUNT 쿼리를 실행하지 않습니다**.

```java
public interface CommunityPostRepository extends JpaRepository<CommunityPostEntity, Long> {

    // Page 대신 Slice 사용 - COUNT 쿼리 자동 실행 방지
    @EntityGraph(attributePaths = {"author", "author.grade"})
    Slice<CommunityPostEntity> findByIsDeletedFalse(Pageable pageable);
}
```

### 총 개수는 Redis에서 캐싱

페이지네이션 UI에 필요한 총 개수는 Redis에서 관리합니다.

```java
@Service
public class CommunityCountService {

    private static final String TOTAL_POST_COUNT_KEY = "community:total_post_count";

    /**
     * 게시글 총 개수 조회 (Redis 캐시 우선)
     */
    public long getTotalPostCount() {
        Long cached = redisService.getTotalPostCount();
        if (cached != null) {
            return cached;
        }

        // Cache Miss - DB에서 조회 후 Redis에 저장
        long count = postRepository.countByIsDeletedFalse();
        redisService.setTotalPostCount(count);
        log.info("Loaded total post count from DB: {}", count);
        return count;
    }

    /**
     * 게시글 생성 시 총 개수 증가
     */
    public void incrementTotalPostCount() {
        redisService.incrementTotalPostCount();
    }

    /**
     * 게시글 삭제 시 총 개수 감소
     */
    public void decrementTotalPostCount() {
        redisService.decrementTotalPostCount();
    }
}
```

### PostService에서 활용

```java
@Service
public class CommunityPostService {

    public Page<CommunityPostResponse> getPosts(Pageable pageable) {
        // 1. Slice로 조회 (COUNT 쿼리 없음)
        Slice<CommunityPostEntity> postsSlice = postRepository.findByIsDeletedFalse(pageable);
        List<CommunityPostEntity> posts = postsSlice.getContent();

        // 2. Bulk 카운터 조회 (MGET - 3번 호출)
        List<Long> postIds = posts.stream().map(CommunityPostEntity::getPostId).toList();
        Map<Long, Integer> viewCounts = countService.getBulkViewCountsFromCache(postIds);
        Map<Long, Integer> likeCounts = countService.getBulkLikeCountsFromCache(postIds);
        Map<Long, Integer> commentCounts = countService.getBulkCommentCountsFromCache(postIds);

        // 3. Response 생성
        List<CommunityPostResponse> responses = posts.stream()
            .map(post -> CommunityPostResponse.fromWithCachedCounts(
                post,
                viewCounts.getOrDefault(post.getPostId(), 0),
                likeCounts.getOrDefault(post.getPostId(), 0),
                commentCounts.getOrDefault(post.getPostId(), 0)
            ))
            .toList();

        // 4. Redis에서 캐싱된 totalCount 사용
        long totalCount = countService.getTotalPostCount();

        return new PageImpl<>(responses, pageable, totalCount);
    }
}
```

---

## 게시글 목록 캐싱 - Cache-Aside 패턴

자주 조회되는 게시글 목록도 Redis에 캐싱합니다.

```java
@Service
public class CommunityPostService {

    private static final String POST_LIST_CACHE_PREFIX = "communityPostList::";
    private static final Duration POST_LIST_CACHE_TTL = Duration.ofMinutes(1);

    public Page<CommunityPostResponse> getPosts(Pageable pageable) {
        String cacheKey = POST_LIST_CACHE_PREFIX + pageable.getPageNumber() + ":" + pageable.getPageSize();

        // 1. 캐시 조회
        String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedJson != null) {
            try {
                List<CommunityPostResponse> cached = objectMapper.readValue(
                    cachedJson, new TypeReference<List<CommunityPostResponse>>() {});

                // 카운터는 항상 최신값으로 조회 (Bulk MGET)
                List<Long> postIds = cached.stream().map(CommunityPostResponse::postId).toList();
                Map<Long, Integer> viewCounts = countService.getBulkViewCountsFromCache(postIds);
                Map<Long, Integer> likeCounts = countService.getBulkLikeCountsFromCache(postIds);
                Map<Long, Integer> commentCounts = countService.getBulkCommentCountsFromCache(postIds);

                // 캐시된 데이터에 최신 카운터 적용
                List<CommunityPostResponse> withLatestCounts = cached.stream()
                    .map(post -> new CommunityPostResponse(
                        post.postId(), post.authorId(), post.authorName(),
                        post.title(), post.content(), post.category(),
                        viewCounts.getOrDefault(post.postId(), 0),
                        likeCounts.getOrDefault(post.postId(), 0),
                        commentCounts.getOrDefault(post.postId(), 0),
                        post.isModified(), post.createdAt(), post.modifiedAt(),
                        post.imagesUrl()
                    ))
                    .toList();

                long totalCount = countService.getTotalPostCount();
                return new PageImpl<>(withLatestCounts, pageable, totalCount);
            } catch (JsonProcessingException e) {
                stringRedisTemplate.delete(cacheKey);
            }
        }

        // 2. Cache Miss - DB 조회 후 캐싱
        // ... (위의 로직과 동일)

        // 3. 캐시 저장
        try {
            String json = objectMapper.writeValueAsString(responses);
            stringRedisTemplate.opsForValue().set(cacheKey, json, POST_LIST_CACHE_TTL);
        } catch (JsonProcessingException e) {
            log.warn("Failed to cache post list", e);
        }

        return new PageImpl<>(responses, pageable, totalCount);
    }
}
```

### 캐시 무효화 - SCAN 사용

게시글이 생성/수정/삭제되면 목록 캐시를 무효화해야 합니다. `KEYS` 명령어는 프로덕션에서 위험하므로 **SCAN**을 사용합니다.

```java
/**
 * 게시글 목록 캐시 무효화 (SCAN 사용 - 프로덕션 안전, 논블로킹)
 */
private void invalidatePostListCache() {
    try {
        var scanOptions = ScanOptions.scanOptions()
            .match(POST_LIST_CACHE_PREFIX + "*")
            .count(100)
            .build();

        int deletedCount = 0;
        try (var cursor = stringRedisTemplate.scan(scanOptions)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                stringRedisTemplate.delete(key);
                deletedCount++;
            }
        }

        if (deletedCount > 0) {
            log.debug("Invalidated {} post list cache entries", deletedCount);
        }
    } catch (Exception e) {
        log.warn("Failed to invalidate post list cache", e);
    }
}
```

---

## 전체 최적화 요약

| 문제 | 해결 방안 | 효과 |
|------|----------|------|
| Full Table Scan | 복합 인덱스 추가 | 스캔 행 50% 감소 |
| JPA N+1 | @EntityGraph | 쿼리 수 대폭 감소 |
| Redis N+1 | Bulk MGET | 30번 → 3번 (90% 감소) |
| COUNT 쿼리 부하 | Slice + Redis 캐싱 | COUNT 쿼리 제거 |
| 반복 조회 부하 | 목록 캐싱 (1분 TTL) | DB 부하 감소 |

---

## 성능 테스트 결과

### 테스트 환경
- k6 부하 테스트
- DAU 30만 기준 시나리오
- 게시글 목록 조회 집중 테스트

### 결과 비교

| 항목 | 2차 개선 | 3차 개선 | 개선율 |
|------|---------|---------|--------|
| 목록 조회 p95 | 800ms | 400ms | **50% 감소** |
| Redis 호출 수 | 30회/요청 | 3회/요청 | **90% 감소** |
| DB 쿼리 수 | 높음 | 낮음 (캐싱) | **대폭 감소** |

---

## 마무리하며

이번 3차 개선을 통해 커뮤니티 게시판의 성능 최적화가 완료되었습니다.

**1차 → 2차 → 3차 개선 요약:**

| 단계 | 주요 개선 | 핵심 기술 |
|------|----------|----------|
| 1차 | 데드락 해결 | JPQL, 네이티브 쿼리, Resource Ordering |
| 2차 | 실시간 카운터 최적화 | Redis, Lua Script, Write-Behind |
| 3차 | 조회 성능 최적화 | 복합 인덱스, Bulk MGET, 캐싱 |

**핵심 교훈:**
- **인덱스 설계**는 쿼리 성능의 기본이다
- **N+1 문제**는 JPA와 Redis 모두에서 발생할 수 있다
- **Bulk 연산**은 네트워크 왕복을 줄이는 핵심 전략이다
- **COUNT 쿼리**는 예상보다 비용이 크다 → 캐싱 필수
- **Cache-Aside 패턴**으로 읽기 성능을 극대화할 수 있다
