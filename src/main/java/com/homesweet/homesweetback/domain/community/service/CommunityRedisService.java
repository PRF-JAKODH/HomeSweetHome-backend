package com.homesweet.homesweetback.domain.community.service;

import com.homesweet.homesweetback.domain.community.config.CommunityConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * [커뮤니티 Redis 서비스 - Redis 저장소 직접 접근 담당]
 *
 * [Redis란?]
 * - 메모리 기반 초고속 데이터 저장소 (1초에 수만 건 처리 가능!)
 * - DB보다 훨씬 빠르지만, 서버 재시작하면 데이터 사라질 수 있음
 * - 그래서 임시 저장소, 캐시 용도로 많이 사용
 *
 * [이 서비스가 하는 일]
 * 1. 조회수, 좋아요수, 댓글수 저장/조회/증가
 * 2. 좋아요 토글 (Set 자료구조 사용)
 * 3. 좋아요 이벤트 큐 관리 (나중에 DB에 동기화할 용도)
 * 4. 스케줄러용 배치 조회 (SCAN 명령어로 패턴 검색)
 *
 * [Redis 자료구조 사용]
 * - String: 조회수, 좋아요수, 댓글수 (숫자 저장)
 * - Set: 좋아요 누른 유저 ID 목록 (중복 방지)
 * - List: 이벤트 큐 (FIFO - 먼저 들어온 게 먼저 처리됨)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityRedisService {

    // Redis 접근용 템플릿 (Object 타입)
    private final RedisTemplate<String, Object> redisTemplate;
    // Redis 접근용 템플릿 (String 타입, 이벤트 큐용)
    private final StringRedisTemplate stringRedisTemplate;
    // 설정값 (TTL 등)
    private final CommunityConfig config;

    // ============================================================
    // [Lua 스크립트 - 원자적 연산 보장]
    //
    // Lua 스크립트란?
    // - Redis 서버에서 직접 실행되는 스크립트
    // - 여러 명령을 하나의 원자적(atomic) 연산으로 묶을 수 있음
    //
    // 왜 필요해?
    // - "읽고 -> 수정하고 -> 쓰기" 과정에서 동시 요청이 오면 데이터가 꼬일 수 있어
    // - Lua 스크립트는 실행 중 다른 명령이 끼어들 수 없어서 안전!
    // ============================================================

    /**
     * [좋아요 토글 스크립트]
     *
     * 동작:
     * 1. 카운트 키가 없으면 -1 반환 (Cache Miss)
     * 2. 유저가 Set에 있으면 -> 제거 + 카운트 -1 + 0 반환
     * 3. 유저가 Set에 없으면 -> 추가 + 카운트 +1 + 1 반환
     */
    private static final String TOGGLE_LIKE_SCRIPT = "if redis.call('EXISTS', KEYS[2]) == 0 then " +
            "  return -1 " + // 키 없으면 -1
            "end " +
            "local isMember = redis.call('SISMEMBER', KEYS[1], ARGV[1]) " + // Set에 유저 있는지 확인
            "if isMember == 1 then " + // 이미 좋아요 누른 상태
            "  redis.call('SREM', KEYS[1], ARGV[1]) " + // Set에서 제거
            "  redis.call('DECR', KEYS[2]) " + // 카운트 -1
            "  return 0 " + // 제거됨 표시
            "else " + // 안 누른 상태
            "  redis.call('SADD', KEYS[1], ARGV[1]) " + // Set에 추가
            "  redis.call('INCR', KEYS[2]) " + // 카운트 +1
            "  return 1 " + // 추가됨 표시
            "end";

    /**
     * [카운터 증가 스크립트]
     * 키가 없으면 -1 반환, 있으면 +1
     */
    private static final String INCREMENT_COUNTER_SCRIPT = "if redis.call('EXISTS', KEYS[1]) == 0 then " +
            "  return -1 " +
            "end " +
            "return redis.call('INCR', KEYS[1])";

    /**
     * [카운터 업데이트 스크립트]
     * 키가 없으면 -1 반환, 있으면 인자값만큼 증가 (음수면 감소)
     */
    private static final String UPDATE_COUNTER_SCRIPT = "if redis.call('EXISTS', KEYS[1]) == 0 then " +
            "  return -1 " +
            "end " +
            "return redis.call('INCRBY', KEYS[1], ARGV[1])";

    // ============================================================
    // [키 생성기 - Redis 키 명명 규칙]
    //
    // 규칙: "도메인:ID:데이터종류"
    // 예: "post:123:viewCount" = 게시글 123번의 조회수
    // ============================================================

    /** 게시글 조회수 키: "post:123:viewCount" */
    private String getPostViewKey(Long postId) {
        return "post:" + postId + ":viewCount";
    }

    /** 게시글 댓글수 키: "post:123:commentCount" */
    private String getPostCommentCountKey(Long postId) {
        return "post:" + postId + ":commentCount";
    }

    /** 게시글 좋아요 Set 키: "post:123:likes" (유저 ID들 저장) */
    private String getPostLikeSetKey(Long postId) {
        return "post:" + postId + ":likes";
    }

    /** 게시글 좋아요 수 키: "post:123:likeCount" */
    private String getPostLikeCountKey(Long postId) {
        return "post:" + postId + ":likeCount";
    }

    /** 댓글 좋아요 Set 키: "comment:456:likes" */
    private String getCommentLikeSetKey(Long commentId) {
        return "comment:" + commentId + ":likes";
    }

    /** 댓글 좋아요 수 키: "comment:456:likeCount" */
    private String getCommentLikeCountKey(Long commentId) {
        return "comment:" + commentId + ":likeCount";
    }

    /** 게시글 좋아요 이벤트 큐 */
    private static final String POST_LIKE_EVENT_QUEUE = "post:like:events";
    /** 댓글 좋아요 이벤트 큐 */
    private static final String COMMENT_LIKE_EVENT_QUEUE = "comment:like:events";

    // ============================================================
    // [TTL(Time To Live) 헬퍼]
    //
    // TTL이란?
    // - 데이터의 유효 기간 (만료되면 자동 삭제됨)
    // - Redis 메모리 관리에 필수!
    // ============================================================

    /** 키에 기본 TTL 설정 */
    private void expireWithDefaultTtl(String key) {
        long ttlSeconds = config.redis().ttl().toSeconds();
        redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
    }

    /** 여러 키에 기본 TTL 설정 */
    private void expireWithDefaultTtl(String... keys) {
        long ttlSeconds = config.redis().ttl().toSeconds();
        for (String key : keys) {
            redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
        }
    }

    // ============================================================
    // [조회수 로직]
    // ============================================================

    /**
     * [조회수 +1]
     * 
     * @return 증가 후 값, -1이면 Cache Miss
     */
    public Long incrementPostViewCount(Long postId) {
        String key = getPostViewKey(postId);
        // Lua 스크립트 실행 (원자적 연산)
        Long result = executeScript(INCREMENT_COUNTER_SCRIPT, Long.class, List.of(key));

        // 성공 시 TTL 갱신 (자주 사용되는 키니까 만료 시간 연장)
        if (result != null && result != -1) {
            expireWithDefaultTtl(key);
        }
        return result;
    }

    /**
     * [조회수 설정]
     * DB에서 값을 가져와서 Redis에 초기 세팅할 때 사용
     */
    public void setPostViewCount(Long postId, int count) {
        String key = getPostViewKey(postId);
        redisTemplate.opsForValue().set(key, count);
        expireWithDefaultTtl(key);
    }

    /**
     * [조회수 조회]
     * 
     * @return 조회수, null이면 Cache Miss
     */
    public Integer getPostViewCount(Long postId) {
        String key = getPostViewKey(postId);
        Integer count = (Integer) redisTemplate.opsForValue().get(key);
        if (count != null) {
            expireWithDefaultTtl(key); // 조회 시에도 TTL 갱신
        }
        return count;
    }

    // ============================================================
    // [댓글수 로직]
    // ============================================================

    /** 댓글수 +1 */
    public Long incrementPostCommentCount(Long postId) {
        String key = getPostCommentCountKey(postId);
        Long result = executeScript(INCREMENT_COUNTER_SCRIPT, Long.class, List.of(key));

        if (result != null && result != -1) {
            expireWithDefaultTtl(key);
        }
        return result;
    }

    /** 댓글수 -1 */
    public Long decreasePostCommentCount(Long postId) {
        String key = getPostCommentCountKey(postId);
        // "-1"을 인자로 전달해서 감소
        Long result = executeScript(UPDATE_COUNTER_SCRIPT, Long.class, List.of(key), "-1");

        if (result != null && result != -1) {
            expireWithDefaultTtl(key);
        }
        return result;
    }

    /** 댓글수 설정 */
    public void setPostCommentCount(Long postId, int count) {
        String key = getPostCommentCountKey(postId);
        redisTemplate.opsForValue().set(key, count);
        expireWithDefaultTtl(key);
    }

    /** 댓글수 조회 */
    public Integer getPostCommentCount(Long postId) {
        String key = getPostCommentCountKey(postId);
        Integer count = (Integer) redisTemplate.opsForValue().get(key);
        if (count != null) {
            expireWithDefaultTtl(key);
        }
        return count;
    }

    // ============================================================
    // [게시글 좋아요 로직]
    // ============================================================

    /**
     * [좋아요 토글]
     * 누르면 추가, 다시 누르면 취소
     *
     * @return 1=추가됨, 0=취소됨, -1=Cache Miss
     */
    public Long togglePostLike(Long postId, Long userId) {
        String likeSetKey = getPostLikeSetKey(postId); // Set 키
        String countKey = getPostLikeCountKey(postId); // 카운트 키

        // Lua 스크립트로 원자적 토글 수행
        Long result = executeScript(TOGGLE_LIKE_SCRIPT, Long.class,
                Arrays.asList(likeSetKey, countKey), userId.toString());

        if (result != null && result != -1) {
            expireWithDefaultTtl(likeSetKey, countKey);
        }
        return result;
    }

    /**
     * [좋아요 목록 세팅]
     * DB에서 좋아요 누른 유저 목록을 가져와서 Redis Set에 저장
     */
    public void setPostLikes(Long postId, List<Long> userIds) {
        String likeSetKey = getPostLikeSetKey(postId);
        String countKey = getPostLikeCountKey(postId);

        // 유저가 있으면 Set에 추가
        if (!userIds.isEmpty()) {
            String[] userIdStrings = userIds.stream().map(String::valueOf).toArray(String[]::new);
            redisTemplate.opsForSet().add(likeSetKey, (Object[]) userIdStrings);
        }
        // 카운트도 설정
        redisTemplate.opsForValue().set(countKey, userIds.size());
        expireWithDefaultTtl(likeSetKey, countKey);
    }

    /**
     * [좋아요 상태 확인]
     * 해당 유저가 이 게시글에 좋아요를 눌렀는지 확인
     */
    public boolean isPostLiked(Long postId, Long userId) {
        // Set에 유저 ID가 있는지 확인
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(getPostLikeSetKey(postId), userId.toString()));
    }

    /**
     * [좋아요 키 존재 여부]
     * Redis에 이 게시글의 좋아요 데이터가 있는지 확인
     */
    public boolean hasPostLikeKey(Long postId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(getPostLikeSetKey(postId)));
    }

    /**
     * [좋아요수 조회]
     */
    public Integer getPostLikeCount(Long postId) {
        String key = getPostLikeCountKey(postId);
        Integer count = (Integer) redisTemplate.opsForValue().get(key);
        if (count != null) {
            expireWithDefaultTtl(key, getPostLikeSetKey(postId));
        }
        return count;
    }

    /**
     * [좋아요 이벤트 기록]
     * 나중에 스케줄러가 DB에 동기화할 때 사용
     *
     * 이벤트 형식: "게시글ID:유저ID:ADD" 또는 "게시글ID:유저ID:REMOVE"
     */
    public void addPostLikeEvent(Long postId, Long userId, boolean isAdded) {
        String event = postId + ":" + userId + ":" + (isAdded ? "ADD" : "REMOVE");
        // List의 오른쪽에 추가 (FIFO 큐)
        stringRedisTemplate.opsForList().rightPush(POST_LIKE_EVENT_QUEUE, event);
    }

    // ============================================================
    // [댓글 좋아요 로직]
    // 게시글 좋아요와 동일한 패턴
    // ============================================================

    /** 댓글 좋아요 토글 */
    public Long toggleCommentLike(Long commentId, Long userId) {
        String likeSetKey = getCommentLikeSetKey(commentId);
        String countKey = getCommentLikeCountKey(commentId);

        Long result = executeScript(TOGGLE_LIKE_SCRIPT, Long.class,
                Arrays.asList(likeSetKey, countKey), userId.toString());

        if (result != null && result != -1) {
            expireWithDefaultTtl(likeSetKey, countKey);
        }
        return result;
    }

    /** 댓글 좋아요 목록 세팅 */
    public void setCommentLikes(Long commentId, List<Long> userIds) {
        String likeSetKey = getCommentLikeSetKey(commentId);
        String countKey = getCommentLikeCountKey(commentId);

        if (!userIds.isEmpty()) {
            String[] userIdStrings = userIds.stream().map(String::valueOf).toArray(String[]::new);
            redisTemplate.opsForSet().add(likeSetKey, (Object[]) userIdStrings);
        }
        redisTemplate.opsForValue().set(countKey, userIds.size());
        expireWithDefaultTtl(likeSetKey, countKey);
    }

    /** 댓글 좋아요 상태 확인 */
    public boolean isCommentLiked(Long commentId, Long userId) {
        return Boolean.TRUE
                .equals(redisTemplate.opsForSet().isMember(getCommentLikeSetKey(commentId), userId.toString()));
    }

    /** 댓글 좋아요 키 존재 여부 */
    public boolean hasCommentLikeKey(Long commentId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(getCommentLikeSetKey(commentId)));
    }

    /** 댓글 좋아요수 조회 */
    public Integer getCommentLikeCount(Long commentId) {
        String key = getCommentLikeCountKey(commentId);
        Integer count = (Integer) redisTemplate.opsForValue().get(key);
        if (count != null) {
            expireWithDefaultTtl(key, getCommentLikeSetKey(commentId));
        }
        return count;
    }

    /** 댓글 좋아요 이벤트 기록 */
    public void addCommentLikeEvent(Long commentId, Long userId, boolean isAdded) {
        String event = commentId + ":" + userId + ":" + (isAdded ? "ADD" : "REMOVE");
        stringRedisTemplate.opsForList().rightPush(COMMENT_LIKE_EVENT_QUEUE, event);
    }

    // ============================================================
    // [벌크 조회 - MGET 사용]
    //
    // MGET이란?
    // - Multiple GET, 여러 키를 한 번에 조회
    // - 10개 조회할 때: GET 10번 vs MGET 1번 -> 훨씬 빠름!
    // ============================================================

    /** 여러 게시글의 조회수 한 번에 조회 */
    public Map<Long, Integer> getBulkViewCounts(List<Long> postIds) {
        return getBulkCounts(postIds, this::getPostViewKey);
    }

    /** 여러 게시글의 좋아요수 한 번에 조회 */
    public Map<Long, Integer> getBulkLikeCounts(List<Long> postIds) {
        return getBulkCounts(postIds, this::getPostLikeCountKey);
    }

    /** 여러 게시글의 댓글수 한 번에 조회 */
    public Map<Long, Integer> getBulkCommentCounts(List<Long> postIds) {
        return getBulkCounts(postIds, this::getPostCommentCountKey);
    }

    /** 여러 댓글의 좋아요수 한 번에 조회 */
    public Map<Long, Integer> getBulkCommentLikeCounts(List<Long> commentIds) {
        return getBulkCounts(commentIds, this::getCommentLikeCountKey);
    }

    /**
     * [벌크 조회 공통 로직]
     * 
     * @param ids       조회할 ID 목록
     * @param keyMapper ID를 Redis 키로 변환하는 함수
     * @return Map<ID, 카운트>
     */
    private Map<Long, Integer> getBulkCounts(List<Long> ids, java.util.function.Function<Long, String> keyMapper) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        // ID 목록을 Redis 키 목록으로 변환
        List<String> keys = ids.stream().map(keyMapper).toList();
        // MGET으로 한 번에 조회
        List<Object> values = redisTemplate.opsForValue().multiGet(keys);

        // 결과를 Map으로 변환
        Map<Long, Integer> result = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            Object value = values != null ? values.get(i) : null;
            result.put(ids.get(i), value != null ? (Integer) value : null);
        }
        return result;
    }

    // ============================================================
    // [스케줄러용 배치 조회 - SCAN 사용]
    //
    // SCAN이란?
    // - 패턴으로 키 검색 (예: "post:*:viewCount")
    // - KEYS 명령어보다 안전 (서버 블로킹 없이 점진적 검색)
    // ============================================================

    /** 모든 게시글의 조회수 수집 (스케줄러용) */
    public Map<Long, Integer> scanAndCollectViewCounts() {
        return scanAndCollect("post:*:viewCount", this::extractPostIdFromKey, this::getPostViewCount);
    }

    /** 모든 게시글의 댓글수 수집 (스케줄러용) */
    public Map<Long, Integer> scanAndCollectCommentCounts() {
        return scanAndCollect("post:*:commentCount", this::extractPostIdFromKey, this::getPostCommentCount);
    }

    /** 모든 게시글의 좋아요수 수집 (스케줄러용) */
    public Map<Long, Integer> scanAndCollectPostLikeCounts() {
        return scanAndCollect("post:*:likeCount", this::extractPostIdFromKey, this::getPostLikeCount);
    }

    /** 모든 댓글의 좋아요수 수집 (스케줄러용) */
    public Map<Long, Integer> scanAndCollectCommentLikeCounts() {
        return scanAndCollect("comment:*:likeCount", this::extractCommentIdFromKey, this::getCommentLikeCount);
    }

    /**
     * [SCAN + 데이터 수집 공통 로직]
     * 
     * @param pattern     검색할 키 패턴 (와일드카드 * 사용)
     * @param idExtractor 키에서 ID 추출 함수
     * @param countGetter ID로 카운트 조회 함수
     * @return Map<ID, 카운트>
     */
    private Map<Long, Integer> scanAndCollect(String pattern,
            java.util.function.Function<String, Long> idExtractor,
            java.util.function.Function<Long, Integer> countGetter) {
        // SCAN 옵션 설정 (패턴 매칭, 한 번에 100개씩)
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
        Map<Long, Integer> result = new HashMap<>();

        // Cursor로 점진적 검색 (try-with-resources로 자동 닫힘)
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                try {
                    // 키에서 ID 추출 (예: "post:123:viewCount" -> 123)
                    Long id = idExtractor.apply(key);
                    // 카운트 조회
                    Integer count = countGetter.apply(id);
                    if (count != null) {
                        result.put(id, count);
                    }
                } catch (Exception e) {
                    log.error("Failed to parse Redis key: {}", key, e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to scan Redis for pattern: {}", pattern, e);
        }
        return result;
    }

    // ============================================================
    // [이벤트 큐 조회 - 스케줄러용]
    //
    // 이벤트 큐란?
    // - 좋아요 추가/취소 이벤트를 임시 저장하는 List
    // - 스케줄러가 주기적으로 읽어서 DB에 반영
    // ============================================================

    /** 게시글 좋아요 이벤트 가져오기 */
    public List<LikeEvent> pollPostLikeEvents(int maxSize) {
        return pollLikeEvents(POST_LIKE_EVENT_QUEUE, maxSize);
    }

    /** 댓글 좋아요 이벤트 가져오기 */
    public List<LikeEvent> pollCommentLikeEvents(int maxSize) {
        return pollLikeEvents(COMMENT_LIKE_EVENT_QUEUE, maxSize);
    }

    /**
     * [이벤트 가져오기 공통 로직]
     * 
     * @param queueKey 큐 이름
     * @param maxSize  최대 가져올 개수
     * @return 이벤트 리스트
     */
    private List<LikeEvent> pollLikeEvents(String queueKey, int maxSize) {
        // List의 0~(maxSize-1) 인덱스 조회
        List<String> events = stringRedisTemplate.opsForList().range(queueKey, 0, maxSize - 1);
        if (events == null || events.isEmpty()) {
            return List.of();
        }
        // 문자열을 LikeEvent 객체로 파싱
        return events.stream()
                .map(this::parseLikeEvent)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * [처리된 이벤트 제거]
     * 처리 완료된 이벤트는 큐에서 잘라냄
     */
    public void trimPostLikeEvents(int processedCount) {
        // processedCount부터 끝까지만 남김 (앞부분 삭제)
        stringRedisTemplate.opsForList().trim(POST_LIKE_EVENT_QUEUE, processedCount, -1);
    }

    public void trimCommentLikeEvents(int processedCount) {
        stringRedisTemplate.opsForList().trim(COMMENT_LIKE_EVENT_QUEUE, processedCount, -1);
    }

    // ============================================================
    // [키 삭제]
    // 스케줄러가 DB 동기화 완료 후 Redis 데이터 정리
    // ============================================================

    public void deletePostViewKey(Long postId) {
        redisTemplate.delete(getPostViewKey(postId));
    }

    public void deletePostCommentCountKey(Long postId) {
        redisTemplate.delete(getPostCommentCountKey(postId));
    }

    public void deletePostLikeKeys(Long postId) {
        redisTemplate.delete(getPostLikeSetKey(postId));
        redisTemplate.delete(getPostLikeCountKey(postId));
    }

    public void deleteCommentLikeKeys(Long commentId) {
        redisTemplate.delete(getCommentLikeSetKey(commentId));
        redisTemplate.delete(getCommentLikeCountKey(commentId));
    }

    // ============================================================
    // [내부 헬퍼 메서드]
    // ============================================================

    /**
     * [Lua 스크립트 실행]
     * 
     * @param scriptText 실행할 Lua 스크립트
     * @param returnType 반환 타입
     * @param keys       스크립트에 전달할 키 목록
     * @param args       스크립트에 전달할 인자
     */
    private <T> T executeScript(String scriptText, Class<T> returnType, List<String> keys, Object... args) {
        DefaultRedisScript<T> script = new DefaultRedisScript<>(scriptText, returnType);
        return redisTemplate.execute(script, keys, args);
    }

    /**
     * [키에서 게시글 ID 추출]
     * "post:123:viewCount" -> 123
     */
    private Long extractPostIdFromKey(String key) {
        String[] parts = key.split(":");
        return Long.parseLong(parts[1]);
    }

    /**
     * [키에서 댓글 ID 추출]
     * "comment:456:likeCount" -> 456
     */
    private Long extractCommentIdFromKey(String key) {
        String[] parts = key.split(":");
        return Long.parseLong(parts[1]);
    }

    /**
     * [이벤트 문자열 파싱]
     * "123:456:ADD" -> LikeEvent(123, 456, true)
     */
    private LikeEvent parseLikeEvent(String event) {
        try {
            String[] parts = event.split(":");
            if (parts.length == 3) {
                return new LikeEvent(
                        Long.parseLong(parts[0]), // targetId (게시글/댓글 ID)
                        Long.parseLong(parts[1]), // userId
                        "ADD".equals(parts[2])); // isAdd
            }
        } catch (Exception e) {
            log.error("Failed to parse like event: {}", event, e);
        }
        return null;
    }

    /**
     * [좋아요 이벤트 DTO]
     * 
     * @param targetId 대상 ID (게시글 또는 댓글)
     * @param userId   유저 ID
     * @param isAdd    true=추가, false=취소
     */
    public record LikeEvent(Long targetId, Long userId, boolean isAdd) {
    }
}