커뮤니티 2차 성능 개선 ‐ Redis 기반 카운터 시스템 구축

[1차 성능 개선](커뮤니티-1차-성능-개선-‐-데드락-문제-해결)에서 JPQL을 도입하여 데드락 문제를 해결했습니다.

하지만 여전히 모든 조회수/좋아요 연산이 DB에 직접 쿼리되는 구조였기 때문에, 트래픽 증가 시 DB가 병목 지점이 될 가능성이 있었습니다.

이번 글에서는 **Redis 기반 카운터 시스템**을 구축하여 DB 부하를 줄이고, **Lua Script**를 활용한 동시성 제어 및 **Write-Behind 패턴**을 적용한 과정을 공유합니다.

---

## 1차 개선 이후 남은 문제

### DB 중심 구조의 한계

```
[클라이언트] → [서버] → [MySQL]
                          ↑
                     모든 카운터 연산이
                     DB에 직접 쿼리됨
```

| 문제점 | 설명 |
|--------|------|
| DB 부하 집중 | 조회수 증가, 좋아요 토글 모두 DB UPDATE |
| 트랜잭션 오버헤드 | 단순 +1 연산에도 트랜잭션 필요 |
| 확장성 한계 | 트래픽 증가 시 DB 커넥션 고갈 위험 |
| 응답 지연 | 디스크 I/O로 인한 지연 |

---

## 해결 방안: Redis 기반 카운터 시스템

### 아키텍처 변경

```
[클라이언트] → [서버] → [Redis] ←──(주기적 동기화)──→ [MySQL]
                          ↑
                    실시간 카운터 처리
                    (조회수, 좋아요, 댓글수)
```

### 핵심 설계 원칙

1. **Redis를 실시간 처리 계층으로 활용**: 카운터 연산은 Redis에서 처리
2. **Lua Script로 원자적 연산 보장**: Race Condition 방지
3. **Write-Behind 패턴**: Redis → DB 비동기 동기화로 내구성 확보
4. **Event Queue 기반 좋아요 관계 동기화**: 배치 처리로 DB 부하 분산

---

## Redis 키 설계

```java
// 게시글 관련 키
private String getPostViewKey(Long postId) { return "post:" + postId + ":viewCount"; }
private String getPostCommentCountKey(Long postId) { return "post:" + postId + ":commentCount"; }
private String getPostLikeSetKey(Long postId) { return "post:" + postId + ":likes"; }
private String getPostLikeCountKey(Long postId) { return "post:" + postId + ":likeCount"; }

// 댓글 관련 키
private String getCommentLikeSetKey(Long commentId) { return "comment:" + commentId + ":likes"; }
private String getCommentLikeCountKey(Long commentId) { return "comment:" + commentId + ":likeCount"; }

// 이벤트 큐 키
private static final String POST_LIKE_EVENT_QUEUE = "post:like:events";
private static final String COMMENT_LIKE_EVENT_QUEUE = "comment:like:events";
```

### 키 구조 설명

| 키 패턴 | 데이터 타입 | 용도 |
|---------|------------|------|
| `post:{id}:viewCount` | String (Integer) | 게시글 조회수 |
| `post:{id}:likeCount` | String (Integer) | 게시글 좋아요 수 |
| `post:{id}:likes` | Set | 좋아요한 유저 ID 집합 |
| `post:like:events` | List | 좋아요 이벤트 큐 (DB 동기화용) |

---

## Lua Script를 활용한 원자적 연산

### 좋아요 토글 Lua Script

```java
private static final String TOGGLE_LIKE_SCRIPT =
    "if redis.call('EXISTS', KEYS[2]) == 0 then " +
    "  return -1 " +  // 키가 없으면 Cache Miss
    "end " +
    "local isMember = redis.call('SISMEMBER', KEYS[1], ARGV[1]) " +
    "if isMember == 1 then " +
    "  redis.call('SREM', KEYS[1], ARGV[1]) " +  // Set에서 제거
    "  redis.call('DECR', KEYS[2]) " +            // 카운트 감소
    "  return 0 " +  // 좋아요 취소
    "else " +
    "  redis.call('SADD', KEYS[1], ARGV[1]) " +  // Set에 추가
    "  redis.call('INCR', KEYS[2]) " +            // 카운트 증가
    "  return 1 " +  // 좋아요 추가
    "end";
```

### Lua Script 사용 이유

| 일반 Redis 명령어 | Lua Script |
|------------------|------------|
| 여러 명령어가 개별 실행 | 모든 명령어가 원자적으로 실행 |
| 중간에 다른 클라이언트 개입 가능 | 실행 중 다른 클라이언트 차단 |
| Race Condition 발생 가능 | Race Condition 완전 방지 |

```
일반 방식의 문제:
1. SISMEMBER (유저 존재 확인)
2. ← 다른 요청이 끼어들 수 있음!
3. SADD/SREM (추가/제거)
4. INCR/DECR (카운트 변경)

Lua Script:
1~4 모두 원자적으로 실행 (중간 개입 불가)
```

---

## CommunityRedisService 구현

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityRedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 게시글 좋아요 토글 (Lua Script 원자적 처리)
     * @return 1: 좋아요 추가, 0: 좋아요 취소, -1: Cache Miss
     */
    public Long togglePostLike(Long postId, Long userId) {
        String likeSetKey = getPostLikeSetKey(postId);
        String countKey = getPostLikeCountKey(postId);

        Long result = executeScript(TOGGLE_LIKE_SCRIPT, Long.class,
                Arrays.asList(likeSetKey, countKey),
                userId.toString());

        // 접근 시 TTL 연장 (7일)
        if (result != null && result != -1) {
            redisTemplate.expire(likeSetKey, 7, TimeUnit.DAYS);
            redisTemplate.expire(countKey, 7, TimeUnit.DAYS);
        }

        return result;
    }

    /**
     * 조회수 증가 (Lua Script 원자적 처리)
     */
    public Long incrementPostViewCount(Long postId) {
        String key = getPostViewKey(postId);
        Long result = executeScript(INCREMENT_COUNTER_SCRIPT, Long.class, List.of(key));

        if (result != null && result != -1) {
            redisTemplate.expire(key, 7, TimeUnit.DAYS);
        }
        return result;
    }

    /**
     * 좋아요 이벤트 큐에 적재 (DB 동기화용)
     */
    public void addPostLikeEvent(Long postId, Long userId, boolean isAdded) {
        String event = postId + ":" + userId + ":" + (isAdded ? "ADD" : "REMOVE");
        stringRedisTemplate.opsForList().rightPush(POST_LIKE_EVENT_QUEUE, event);
    }
}
```

---

## CommunityCountService - Cache-Aside 패턴

```java
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityCountService {

    private final CommunityRedisService redisService;
    private final CommunityPostRepository postRepository;
    private final CommunityPostLikeRepository postLikeRepository;

    /**
     * 조회수 증가 - Redis 우선, Cache Miss 시 DB에서 로딩
     */
    public void increaseViewCount(Long postId) {
        Long result = redisService.incrementPostViewCount(postId);

        // Cache Miss 처리 (Lazy Loading)
        if (result == -1) {
            initViewCountFromDB(postId);
            redisService.incrementPostViewCount(postId);
        }

        log.debug("View count increased - postId: {}", postId);
    }

    /**
     * 좋아요 토글 - Redis에서 처리 + Event Queue 적재
     */
    public void togglePostLike(Long postId, Long userId) {
        Long result = redisService.togglePostLike(postId, userId);

        // Cache Miss 처리
        if (result == -1) {
            initPostLikesFromDB(postId);
            result = redisService.togglePostLike(postId, userId);
        }

        boolean isAdded = (result == 1);

        // Event Queue에 적재 (DB 동기화용)
        redisService.addPostLikeEvent(postId, userId, isAdded);

        log.debug("Post like toggled - postId: {}, userId: {}, action: {}",
                postId, userId, isAdded ? "ADDED" : "REMOVED");
    }

    /**
     * Cache Miss 시 DB에서 좋아요 데이터 로딩
     */
    private void initPostLikesFromDB(Long postId) {
        List<Long> userIds = postLikeRepository.findUserIdsByPostId(postId);
        redisService.setPostLikes(postId, userIds);
        log.info("Loaded post likes from DB - postId: {}, count: {}", postId, userIds.size());
    }
}
```

---

## Write-Behind 패턴 - 스케줄러 구현

Redis의 데이터를 주기적으로 DB에 동기화하여 **내구성(Durability)**을 확보합니다.

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class CommunityScheduler {

    private final CommunityRedisService redisService;
    private final CommunityPostRepository postRepository;
    private final CommunityPostLikeRepository postLikeRepository;
    private final TransactionTemplate transactionTemplate;

    /**
     * 조회수 동기화 - 약 2분마다 실행
     */
    @Scheduled(initialDelay = 100000, fixedDelay = 110000)
    public void updateCountData() {
        Map<Long, Integer> viewCounts = redisService.scanAndCollectViewCounts();

        if (viewCounts.isEmpty()) return;

        transactionTemplate.executeWithoutResult(status -> {
            viewCounts.forEach((postId, viewCount) -> {
                try {
                    int affected = postRepository.updateViewCount(postId, viewCount);
                    if (affected == 0) {
                        // 게시글이 삭제된 경우 Redis 키 정리
                        redisService.deletePostViewKey(postId);
                    }
                } catch (Exception e) {
                    log.error("Failed to update view count for postId: {}", postId, e);
                }
            });
        });

        log.info("View count sync completed - total processed: {}", viewCounts.size());
    }

    /**
     * 좋아요 이벤트 배치 동기화 - 5분마다 실행
     */
    @Scheduled(fixedDelay = 300000)
    public void syncPostLikeEvents() {
        List<LikeEvent> events = redisService.pollPostLikeEvents(1000);

        if (events.isEmpty()) return;

        // 마지막 상태만 추출 (중복 제거)
        Map<String, LikeEvent> deduplicated = events.stream()
                .collect(Collectors.toMap(
                        e -> e.targetId() + ":" + e.userId(),
                        e -> e,
                        (old, new_) -> new_  // 마지막 이벤트만 유지
                ));

        int[] counts = {0, 0};
        transactionTemplate.executeWithoutResult(status -> {
            deduplicated.values().forEach(event -> {
                try {
                    if (event.isAdd()) {
                        postLikeRepository.insertPostLike(event.targetId(), event.userId());
                        counts[0]++;
                    } else {
                        postLikeRepository.deleteByPostIdAndUserId(event.targetId(), event.userId());
                        counts[1]++;
                    }
                } catch (Exception e) {
                    log.error("Failed to sync post like event: {}", event, e);
                }
            });
        });

        // 처리된 이벤트 제거
        redisService.trimPostLikeEvents(events.size());

        log.info("Post like events synced - added: {}, removed: {}", counts[0], counts[1]);
    }
}
```

---

## 캐시 워밍업 스케줄러

Cold Start 문제를 방지하기 위해 서버 시작 시 인기 게시글 데이터를 미리 로딩합니다.

```java
/**
 * 인기 게시글 캐시 워밍업 - 서버 시작 시 및 1시간마다 실행
 */
@Scheduled(initialDelay = 5000, fixedDelay = 3600000)
public void warmupPopularPostsCache() {
    log.info("Starting cache warmup for popular posts...");

    try {
        // 최근 게시글 100개 조회
        var recentPosts = postRepository.findByIsDeletedFalse(
                PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "postId"))
        );

        int warmupCount = 0;
        for (var post : recentPosts.getContent()) {
            Long postId = post.getPostId();

            // 좋아요 데이터 워밍업
            if (!redisService.hasPostLikeKey(postId)) {
                List<Long> userIds = postLikeRepository.findUserIdsByPostId(postId);
                redisService.setPostLikes(postId, userIds);
                warmupCount++;
            }

            // 카운터 워밍업
            countService.getViewCountFromCache(postId);
            countService.getLikeCountFromCache(postId);
            countService.getCommentCountFromCache(postId);
        }

        log.info("Cache warmup completed - posts: {}, likes warmed: {}",
                recentPosts.getContent().size(), warmupCount);
    } catch (Exception e) {
        log.error("Failed to warmup cache", e);
    }
}
```

---

## 전체 아키텍처 요약

```
┌─────────────────────────────────────────────────────────────────┐
│                        클라이언트 요청                           │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    CommunityCountService                        │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 1. Redis 조회 (Cache Hit → 즉시 반환)                    │   │
│  │ 2. Cache Miss → DB에서 로딩 후 Redis 저장               │   │
│  │ 3. Lua Script로 원자적 카운터 연산                       │   │
│  │ 4. Event Queue에 이벤트 적재                            │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                                │
                ┌───────────────┴───────────────┐
                ▼                               ▼
┌───────────────────────────┐   ┌───────────────────────────────┐
│          Redis            │   │     CommunityScheduler        │
│  ┌─────────────────────┐  │   │  ┌─────────────────────────┐  │
│  │ post:1:viewCount    │  │   │  │ 2분마다: 조회수 동기화   │  │
│  │ post:1:likeCount    │  │   │  │ 5분마다: 좋아요 동기화   │  │
│  │ post:1:likes (Set)  │  │   │  │ 1시간마다: 캐시 워밍업   │  │
│  │ post:like:events    │  │   │  └─────────────────────────┘  │
│  └─────────────────────┘  │   └───────────────────────────────┘
└───────────────────────────┘                   │
                                                ▼
                                ┌───────────────────────────────┐
                                │           MySQL               │
                                │  (영구 저장소, 이력 보존)      │
                                └───────────────────────────────┘
```

---

## 성능 테스트 결과

### 테스트 환경
- k6 부하 테스트
- DAU 30만 기준 시나리오
- 2000 VU 동시 접속

### 결과 비교

| 항목 | 1차 개선 (JPQL) | 2차 개선 (Redis) | 개선율 |
|------|----------------|-----------------|--------|
| 평균 응답 시간 | 38ms | 17ms | **55% 감소** |
| p95 응답 시간 | 65ms | 21ms | **67% 감소** |
| DB 쿼리 수/초 | 높음 | 낮음 (배치 동기화) | **대폭 감소** |
| 동시성 에러 | 0 | 0 | 유지 |

---

## 아직 남은 문제

Redis 기반 카운터 시스템으로 성능이 크게 향상되었지만, 여전히 개선할 부분이 있습니다:

1. **게시글 목록 조회 시 N+1 문제**: 각 게시글마다 Redis 호출
2. **DB COUNT 쿼리 부하**: 페이지네이션 시 전체 개수 조회
3. **복합 인덱스 부재**: 게시글 목록 조회 시 Full Table Scan

이러한 문제들은 **[3차 성능 개선 (DB 쿼리 최적화 및 캐싱 전략)](커뮤니티-3차-성능-개선-‐-DB-쿼리-최적화-및-캐싱-전략)**에서 해결합니다.

---

## 마무리하며

이번 개선을 통해 **"DB에서 처리할 것과 캐시에서 처리할 것을 분리"**하는 것이 얼마나 중요한지 깨달았습니다.

**핵심 교훈:**
- 실시간 카운터는 **Redis가 적합** (빠른 응답, 원자적 연산)
- Lua Script로 **Race Condition 완전 방지** 가능
- Write-Behind 패턴으로 **성능과 내구성 모두 확보**
- Event Queue로 **DB 부하 분산** (배치 처리)
- 캐시 워밍업으로 **Cold Start 문제 방지**
