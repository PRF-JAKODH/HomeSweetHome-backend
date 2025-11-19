# 데드락 완전 분석 가이드

다른 사람에게 설명할 수 있도록 데드락의 모든 것을 정리했습니다.

---

## 📋 목차
1. [발생한 데드락 로그 분석](#1-발생한-데드락-로그-분석)
2. [MySQL InnoDB 락 메커니즘](#2-mysql-innodb-락-메커니즘)
3. [데드락 발생 원인 (우리 코드)](#3-데드락-발생-원인-우리-코드)
4. [데드락 발생 시나리오 타임라인](#4-데드락-발생-시나리오-타임라인)
5. [왜 비관적 락으로 해결이 안 되는가?](#5-왜-비관적-락으로-해결이-안-되는가)
6. [해결 방법 비교](#6-해결-방법-비교)

---

## 1. 발생한 데드락 로그 분석

### 1.1 핵심 에러 메시지
```
[2025-11-19 16:24:38] WARN  [http-nio-8080-exec-3]
SQL Error: 1213, SQLState: 40001

[2025-11-19 16:24:38] ERROR [http-nio-8080-exec-3]
Deadlock found when trying to get lock; try restarting transaction
```

### 1.2 에러 코드 분석
| 코드 | 의미 |
|------|------|
| **1213** | MySQL Deadlock Error Code |
| **40001** | SQL State: Transaction Rollback (Deadlock) |

### 1.3 실패한 SQL 쿼리
```sql
UPDATE community_posts
SET user_id=?, category=?, comment_count=?, content=?,
    is_deleted=?, is_modified=?, like_count=?, modified_at=?,
    title=?, view_count=?
WHERE post_id=?
```

**분석**: JPA의 Dirty Checking으로 인한 UPDATE 쿼리가 커밋 시점에 실행됩니다.

### 1.4 로그에서 찾은 동시 요청
```
[16:24:38] DEBUG [http-nio-8080-exec-3] POST /api/v1/community/posts/1/comments
[16:24:38] DEBUG [http-nio-8080-exec-6] POST /api/v1/community/posts/1/comments
                                        ↑ 같은 post_id=1에 동시 요청!
```

---

## 2. MySQL InnoDB 락 메커니즘

### 2.1 InnoDB의 기본 설정
```bash
$ docker exec homesweet-db mysql -u user -ppassword -e "SELECT @@transaction_isolation;"
```
```
@@transaction_isolation
REPEATABLE-READ          ← MySQL 기본값
```

### 2.2 Isolation Level: REPEATABLE-READ

**REPEATABLE-READ**는 MySQL InnoDB의 기본 격리 수준입니다.

#### 특징:
1. **같은 트랜잭션 내에서 같은 SELECT는 항상 같은 결과를 반환**
2. **Phantom Read 방지를 위해 Gap Lock 사용**
3. **UPDATE/DELETE 시 해당 row에 X-Lock (배타적 락) 설정**

### 2.3 락의 종류

#### 2.3.1 Shared Lock (S-Lock, 공유 락)
```sql
SELECT ... FOR SHARE
-- 또는
SELECT ... LOCK IN SHARE MODE
```
- 읽기 락
- 다른 트랜잭션도 S-Lock 획득 가능
- 다른 트랜잭션이 X-Lock 획득 불가

#### 2.3.2 Exclusive Lock (X-Lock, 배타적 락)
```sql
SELECT ... FOR UPDATE        -- 명시적
UPDATE ... WHERE ...         -- 암묵적
DELETE ... WHERE ...         -- 암묵적
```
- 쓰기 락
- 다른 트랜잭션이 S-Lock, X-Lock 모두 획득 불가
- **완전히 독점**

#### 2.3.3 Gap Lock
```sql
-- 예: id BETWEEN 10 AND 20 범위 락
SELECT * FROM table WHERE id > 10 AND id < 20 FOR UPDATE
```
- **범위 락** (레코드 사이의 "갭"을 잠금)
- Phantom Read 방지
- REPEATABLE-READ 이상에서만 동작

### 2.4 락 대기 메커니즘

```
트랜잭션 A                      트랜잭션 B
───────────────────────────────────────────────────────
SELECT ... FOR UPDATE (row 1)
  → X-Lock 획득 ✓
                                SELECT ... FOR UPDATE (row 1)
                                  → X-Lock 대기... ⏳
                                  (트랜잭션 A가 커밋할 때까지)
COMMIT
  → X-Lock 해제 ✓
                                  → X-Lock 획득 ✓
                                COMMIT
```

### 2.5 데드락 발생 조건 (4가지 모두 충족 시)

1. **Mutual Exclusion (상호 배제)**: 리소스는 한 번에 한 프로세스만 사용
2. **Hold and Wait (보유 및 대기)**: 자원을 보유한 채 다른 자원 대기
3. **No Preemption (비선점)**: 강제로 리소스를 빼앗을 수 없음
4. **Circular Wait (순환 대기)**: 대기 그래프에 사이클 형성

---

## 3. 데드락 발생 원인 (우리 코드)

### 3.1 문제의 코드: CommunityCommentService.createComment()

**파일**: `CommunityCommentService.java:35-72`

```java
@Transactional
public CommunityCommentResponse createComment(Long postId, ...) {
    // 1. 락 없이 게시글 조회 ⚠️
    CommunityPostEntity post = postRepository
        .findByPostIdAndIsDeletedFalse(postId)  // ← SELECT (락 없음!)
        .orElseThrow(...);

    // 2. 댓글 저장
    CommunityCommentEntity comment = commentRepository.save(...);

    // 3. 게시글의 댓글 수 증가 (Dirty Checking) ⚠️
    post.increaseCommentCount();  // ← commentCount++

    // 4. 알림 전송 (트랜잭션 내부에서!) ⚠️
    notificationSendService.sendTemplateNotificationToSingleUser(...);

    // 5. 트랜잭션 커밋 시 UPDATE 실행 ⚠️
    // → UPDATE community_posts SET comment_count=? WHERE post_id=?
}
```

### 3.2 실행되는 실제 SQL

```sql
-- Step 1: 게시글 조회 (락 없음!)
SELECT * FROM community_posts WHERE post_id = 1 AND is_deleted = false;

-- Step 2: 댓글 저장
INSERT INTO community_comments (post_id, author_id, content, ...) VALUES (...);

-- Step 3: 커밋 시 Dirty Checking으로 UPDATE 실행
UPDATE community_posts
SET comment_count = comment_count + 1, updated_at = NOW()
WHERE post_id = 1;
```

### 3.3 왜 락이 없나?

**`findByPostIdAndIsDeletedFalse(postId)`는 일반 SELECT입니다:**
```sql
SELECT * FROM community_posts WHERE post_id = ? AND is_deleted = false
```

**락을 걸려면 명시적으로 지정해야 합니다:**
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)  // ← 이것이 필요!
Optional<CommunityPostEntity> findByPostIdAndIsDeletedFalseWithPessimisticLock(Long postId);
```

이것은 다음 SQL로 변환됩니다:
```sql
SELECT * FROM community_posts WHERE post_id = ? AND is_deleted = false FOR UPDATE
                                                                        ^^^^^^^^^^^
```

---

## 4. 데드락 발생 시나리오 타임라인

### 4.1 시간순 이벤트 (밀리초 단위)

```
시간 (ms)    트랜잭션 A (exec-3)                    트랜잭션 B (exec-6)
──────────────────────────────────────────────────────────────────────────────
0           POST /posts/1/comments 요청 시작
1                                                  POST /posts/1/comments 요청 시작
10          BEGIN TRANSACTION
11                                                 BEGIN TRANSACTION

20          SELECT * FROM community_posts
            WHERE post_id = 1
            → 결과: comment_count = 5

25                                                 SELECT * FROM community_posts
                                                   WHERE post_id = 1
                                                   → 결과: comment_count = 5

30          INSERT INTO community_comments ...
            (댓글 저장)

35          post.increaseCommentCount()
            → commentCount = 6 (메모리에만)

40                                                 INSERT INTO community_comments ...
                                                   (댓글 저장)

45                                                 post.increaseCommentCount()
                                                   → commentCount = 6 (메모리에만)

50          notificationSendService.send(...)
            → 알림 이벤트 발행 (비동기)

55                                                 notificationSendService.send(...)
                                                   → 알림 이벤트 발행 (비동기)

100         COMMIT 시도
            → Dirty Checking 감지
            → UPDATE community_posts
              SET comment_count = 6
              WHERE post_id = 1
            → X-Lock 획득 시도 ⏳

105                                                COMMIT 시도
                                                   → Dirty Checking 감지
                                                   → UPDATE community_posts
                                                     SET comment_count = 6
                                                     WHERE post_id = 1
                                                   → X-Lock 획득 시도 ⏳

110         (대기 중... B가 끝나길 기다림)

115                                                (대기 중... A가 끝나길 기다림)

120         💥 DEADLOCK DETECTED 💥
            ← MySQL이 순환 대기 감지!
            → 트랜잭션 A를 VICTIM으로 선택하여 롤백
            → SQL Error: 1213

125                                                → COMMIT 성공 ✓
                                                   (comment_count = 6으로 업데이트)

130         Exception 발생:
            "Deadlock found when trying to get lock"
```

### 4.2 MySQL 내부의 락 대기 그래프

```
트랜잭션 A                    트랜잭션 B
    │                             │
    │  UPDATE community_posts     │
    │  WHERE post_id = 1          │
    │  (X-Lock 시도)              │
    │         │                   │
    │         ▼                   │
    │    ┌─────────┐              │
    │    │ Row: 1  │◄─────────────┼── UPDATE community_posts
    │    └─────────┘              │   WHERE post_id = 1
    │         │                   │   (X-Lock 시도)
    │         │                   │
    │    대기 관계 형성            │
    │    (Circular Wait!)         │
    └─────────────────────────────┘
              순환 대기
           → DEADLOCK!
```

### 4.3 Lost Update 문제도 발생!

**데드락이 발생하지 않더라도, Lost Update 문제가 발생합니다:**

```
초기 상태: comment_count = 5

트랜잭션 A: SELECT → comment_count = 5
트랜잭션 B: SELECT → comment_count = 5

트랜잭션 A: UPDATE comment_count = 6 → COMMIT
트랜잭션 B: UPDATE comment_count = 6 → COMMIT

최종 상태: comment_count = 6  (❌ 잘못됨! 7이어야 함!)
```

---

## 5. 왜 비관적 락으로 해결이 안 되는가?

### 5.1 현재 시도 (일부만 락 사용)

**CommunityCountService.java** - 비관적 락 사용:
```java
@Transactional
public void increaseViewCount(Long postId) {
    CommunityPostEntity post = postRepository
        .findByPostIdAndIsDeletedFalseWithPessimisticLock(postId);  // FOR UPDATE
    post.increaseViewCount();
}
```

**CommunityCommentService.java** - 락 없음:
```java
@Transactional
public CommunityCommentResponse createComment(Long postId, ...) {
    CommunityPostEntity post = postRepository
        .findByPostIdAndIsDeletedFalse(postId);  // 일반 SELECT (락 없음!)
    post.increaseCommentCount();
}
```

### 5.2 문제점: 일관성 없는 락 사용

```
increaseViewCount()는 FOR UPDATE 사용
     ↓
   락 획득

createComment()는 일반 SELECT 사용
     ↓
   락 없음 → 데드락 발생!
```

### 5.3 모든 곳에 비관적 락을 사용하면?

**문제 1: 성능 저하**
```
동시 사용자 100명이 같은 게시글 조회
  → 모두 순차적으로 대기 (직렬화)
  → 응답 시간 급증!
```

**문제 2: 락 보유 시간 증가**
```java
@Transactional
public void createComment(...) {
    // 1. X-Lock 획득
    CommunityPostEntity post = postRepository
        .findByPostIdAndIsDeletedFalseWithPessimisticLock(postId);

    // 2. 댓글 저장 (DB I/O)
    commentRepository.save(...);

    // 3. 알림 전송 (외부 작업) ← 락을 잡은 채로!
    notificationSendService.send(...);

    // 4. 커밋
    // → 락 보유 시간이 너무 길어짐!
}
```

**문제 3: Deadlock 여전히 발생 가능**

비관적 락을 사용해도 여러 row를 수정하거나, 락 획득 순서가 다르면 데드락 발생:
```
트랜잭션 A: Lock Row 1 → Lock Row 2
트랜잭션 B: Lock Row 2 → Lock Row 1
                  → DEADLOCK!
```

---

## 6. 해결 방법 비교

### 방법 1: 낙관적 락 (Optimistic Lock) ⭐ 추천

#### 원리
```java
@Entity
public class CommunityPostEntity {
    @Version
    private Long version;  // 버전 필드 추가
}
```

#### 동작 방식
```sql
-- 조회 시
SELECT post_id, comment_count, version FROM community_posts WHERE post_id = 1;
-- 결과: comment_count=5, version=10

-- 업데이트 시
UPDATE community_posts
SET comment_count = 6, version = 11
WHERE post_id = 1 AND version = 10;  ← version 체크!

-- 다른 트랜잭션이 먼저 업데이트했다면:
-- → Affected Rows = 0
-- → OptimisticLockException 발생
```

#### 장점
- ✅ 락을 걸지 않아 **성능 우수**
- ✅ 읽기가 많은 시스템에 **최적**
- ✅ 데드락 발생하지 않음

#### 단점
- ❌ 충돌 시 재시도 로직 필요
- ❌ 쓰기가 많으면 충돌 빈번

#### 구현
```java
@Entity
public class CommunityPostEntity extends BaseEntity {
    @Id
    private Long postId;

    @Version  // ← 추가!
    @Column(nullable = false)
    private Long version = 0L;

    // ... 나머지 필드
}

// 재시도 로직
@Transactional
public void createCommentWithRetry(Long postId, ...) {
    int maxRetries = 3;
    for (int i = 0; i < maxRetries; i++) {
        try {
            createComment(postId, ...);
            return;
        } catch (OptimisticLockException e) {
            if (i == maxRetries - 1) throw e;
            // 재시도
        }
    }
}
```

---

### 방법 2: Redis 카운터 ⭐⭐ 추천 (높은 동시성)

#### 원리
```java
// DB 대신 Redis로 카운트 관리
redisTemplate.opsForHash().increment("post:1", "viewCount", 1);
redisTemplate.opsForHash().increment("post:1", "commentCount", 1);
```

#### 동작 방식
```
댓글 생성 요청
  ↓
Redis HINCRBY post:1 commentCount 1  (원자적 연산)
  ↓
DB에 댓글만 저장 (게시글은 건드리지 않음!)
  ↓
주기적으로 Redis → DB 동기화 (배치)
```

#### 장점
- ✅ **극도로 빠름** (메모리 연산)
- ✅ Redis의 원자적 연산으로 **완벽한 동시성 제어**
- ✅ DB 부하 감소
- ✅ 데드락 원천 차단

#### 단점
- ❌ Redis 장애 시 데이터 유실 가능 (AOF/RDB로 완화)
- ❌ 동기화 배치 구현 필요
- ❌ 실시간 정확성이 조금 떨어질 수 있음 (배치 주기에 따라)

#### 구현
```java
@Service
@RequiredArgsConstructor
public class RedisCommunityCountService {
    private final RedisTemplate<String, String> redisTemplate;

    public void increaseCommentCount(Long postId) {
        String key = "post:" + postId;
        redisTemplate.opsForHash().increment(key, "commentCount", 1);
    }

    public Long getCommentCount(Long postId) {
        String key = "post:" + postId;
        Object count = redisTemplate.opsForHash().get(key, "commentCount");
        return count != null ? Long.parseLong(count.toString()) : 0L;
    }
}

// 배치 동기화 (1분마다)
@Scheduled(fixedRate = 60000)
public void syncCountsToDatabase() {
    // Redis의 카운터를 DB에 일괄 반영
}
```

---

### 방법 3: 트랜잭션 이벤트 분리

#### 원리
```java
// 알림 전송을 트랜잭션 밖으로!
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleCommentCreated(CommentCreatedEvent event) {
    notificationSendService.send(...);
}
```

#### 장점
- ✅ 락 보유 시간 단축
- ✅ 외부 작업이 트랜잭션에 영향 없음

#### 단점
- ❌ 근본적인 데드락 해결은 아님

---

### 방법 4: DB Native Query로 Atomic Update

#### 원리
```java
@Modifying
@Query("UPDATE CommunityPostEntity p SET p.commentCount = p.commentCount + 1 WHERE p.postId = :postId")
void incrementCommentCount(@Param("postId") Long postId);
```

```sql
UPDATE community_posts
SET comment_count = comment_count + 1
WHERE post_id = ?
```

#### 장점
- ✅ 단일 쿼리로 원자적 업데이트
- ✅ Lost Update 방지
- ✅ SELECT 불필요

#### 단점
- ❌ JPA의 1차 캐시와 불일치 가능
- ❌ Dirty Checking 무시됨

---

## 7. 최종 권장 사항

### 시나리오별 권장 방법

| 상황 | 추천 방법 | 이유 |
|------|----------|------|
| **조회수/좋아요** (쓰기 빈번, 정확성 덜 중요) | Redis 카운터 | 극도의 성능, 실시간성 덜 중요 |
| **댓글 수** (쓰기 적당, 정확성 중요) | 낙관적 락 + 재시도 | 성능과 정확성 밸런스 |
| **재고 관리** (정확성 필수) | 비관적 락 | 데이터 일관성 최우선 |
| **배치 작업** | DB Native Query | 대량 업데이트에 효율적 |

### 우리 프로젝트 권장 구조

```java
// 조회수 - Redis
public void increaseViewCount(Long postId) {
    redisTemplate.opsForHash().increment("post:" + postId, "viewCount", 1);
}

// 좋아요 수 - Redis
public void togglePostLike(Long postId, Long userId) {
    if (좋아요 추가) {
        redisTemplate.opsForHash().increment("post:" + postId, "likeCount", 1);
    } else {
        redisTemplate.opsForHash().increment("post:" + postId, "likeCount", -1);
    }
}

// 댓글 수 - 낙관적 락 + Native Query
@Transactional
public void createComment(Long postId, ...) {
    // 댓글 저장
    commentRepository.save(...);

    // Atomic Update
    postRepository.incrementCommentCount(postId);

    // 알림은 이벤트로 분리
    eventPublisher.publishEvent(new CommentCreatedEvent(...));
}
```

---

## 8. 참고 자료

- [MySQL InnoDB Locking](https://dev.mysql.com/doc/refman/8.0/en/innodb-locking.html)
- [JPA Optimistic Locking](https://docs.oracle.com/javaee/7/tutorial/persistence-locking002.htm)
- [Spring Transaction Management](https://docs.spring.io/spring-framework/reference/data-access/transaction.html)
