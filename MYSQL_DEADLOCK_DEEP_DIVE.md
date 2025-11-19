# MySQL InnoDB 데드락 심층 분석 (실제 로그 기반)

## 🔍 실제 MySQL에서 발생한 데드락 로그 분석

### 1. 데드락 발생 시간
```
2025-11-19 07:36:26 (UTC)
→ 한국 시간: 16:36:26
```

---

## 📊 데드락 상세 정보

### 트랜잭션 1 (희생양이 아님)
```
*** (1) TRANSACTION:
TRANSACTION 94077, ACTIVE 0 sec starting index read
mysql tables in use 1, locked 1
LOCK WAIT 7 lock struct(s), heap size 1128, 3 row lock(s), undo log entries 1
MySQL thread id 154, OS thread handle 281472899788544, query id 956732
```

**분석:**
- **Transaction ID**: 94077
- **Thread ID**: 154 (HTTP 요청 스레드)
- **상태**: ACTIVE 0 sec (막 시작한 직후)
- **락 상태**: LOCK WAIT (락을 기다리는 중)
- **락 구조**: 7개의 lock struct
- **보유한 row lock**: 3개

**실행한 쿼리:**
```sql
UPDATE community_posts
SET user_id=9999,
    category='GENERAL',
    comment_count=5293,
    content='이것은 k6 부하 테스트를 위한 샘플 게시글입니다.',
    is_deleted=0,
    is_modified=0,
    like_count=-67,
    modified_at=null,
    title='부하 테스트용 게시글 1',
    view_count=13393
WHERE post_id=1
```

---

### 트랜잭션 1이 보유한 락
```
*** (1) HOLDS THE LOCK(S):
RECORD LOCKS space id 77 page no 6 n bits 192
index PRIMARY of table `homesweet`.`community_posts`
trx id 94077 lock mode S locks rec but not gap
                ^^^^^^^^^^^^^^^^
                S-Lock (공유 락)
```

**핵심:**
- **락 타입**: `lock mode S` = **Shared Lock (공유 락)**
- **락 범위**: `locks rec but not gap` = 레코드만 잠금 (갭 락 없음)
- **테이블**: `community_posts`
- **인덱스**: PRIMARY (post_id=1)

**S-Lock (공유 락)이란?**
- 여러 트랜잭션이 **동시에 읽기** 가능
- 하지만 **쓰기는 불가능**
- `SELECT ... FOR SHARE` 또는 특정 상황에서 자동 획득

---

### 트랜잭션 1이 대기 중인 락
```
*** (1) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 77 page no 6 n bits 192
index PRIMARY of table `homesweet`.`community_posts`
trx id 94077 lock_mode X locks rec but not gap waiting
                ^^^^^^^^^^
                X-Lock (배타적 락) 대기!
```

**핵심:**
- **원하는 락**: `lock_mode X` = **Exclusive Lock (배타적 락)**
- **상태**: `waiting` = 대기 중
- **이유**: UPDATE를 실행하려면 X-Lock이 필요한데, 다른 트랜잭션이 S-Lock을 보유 중

---

### 트랜잭션 2 (희생양 - 롤백됨)
```
*** (2) TRANSACTION:
TRANSACTION 94070, ACTIVE 0 sec starting index read
mysql tables in use 1, locked 1
LOCK WAIT 7 lock struct(s), heap size 1128, 3 row lock(s), undo log entries 1
MySQL thread id 156, OS thread handle 281473032953600, query id 956731
```

**분석:**
- **Transaction ID**: 94070
- **Thread ID**: 156 (다른 HTTP 요청 스레드)
- **실행한 쿼리**: 트랜잭션 1과 **완전히 동일**

---

### 트랜잭션 2가 보유한 락
```
*** (2) HOLDS THE LOCK(S):
RECORD LOCKS space id 77 page no 6 n bits 192
index PRIMARY of table `homesweet`.`community_posts`
trx id 94070 lock mode S locks rec but not gap
                ^^^^^^^^^^^^^^^^
                S-Lock (공유 락)
```

**트랜잭션 2도 S-Lock을 보유!**

---

### 트랜잭션 2가 대기 중인 락
```
*** (2) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 77 page no 6 n bits 192
index PRIMARY of table `homesweet`.`community_posts`
trx id 94070 lock_mode X locks rec but not gap waiting
                ^^^^^^^^^^
                X-Lock (배타적 락) 대기!
```

---

### 데드락 해결
```
*** WE ROLL BACK TRANSACTION (2)
```

**MySQL의 선택:**
- 트랜잭션 2 (94070)를 희생양(Victim)으로 선택
- 트랜잭션 2를 **ROLLBACK**
- 트랜잭션 1은 계속 진행

---

## 🎯 데드락 발생 원인 분석

### 핵심 문제: S-Lock → X-Lock 업그레이드 충돌

#### 시나리오 타임라인

```
시간   트랜잭션 1 (Thread 154)              트랜잭션 2 (Thread 156)
─────────────────────────────────────────────────────────────────────
0ms    BEGIN TRANSACTION
1ms                                         BEGIN TRANSACTION

10ms   어떤 작업으로 S-Lock 획득
       (아마도 SELECT ... FOR SHARE)
       → S-Lock 보유 ✓

20ms                                        어떤 작업으로 S-Lock 획득
                                            → S-Lock 보유 ✓
                                            (여러 트랜잭션이 S-Lock 동시 보유 가능!)

30ms   UPDATE 시도!
       → X-Lock 필요
       → S-Lock → X-Lock 업그레이드 시도
       → ❌ 트랜잭션 2가 S-Lock 보유 중
       → 대기... ⏳

40ms                                        UPDATE 시도!
                                            → X-Lock 필요
                                            → S-Lock → X-Lock 업그레이드 시도
                                            → ❌ 트랜잭션 1이 S-Lock 보유 중
                                            → 대기... ⏳

50ms   💥 순환 대기 감지! 💥

       트랜잭션 1: S-Lock 보유, X-Lock 대기 (트랜잭션 2 때문에)
                   ↓
       트랜잭션 2: S-Lock 보유, X-Lock 대기 (트랜잭션 1 때문에)
                   ↑

       → DEADLOCK!

60ms   MySQL이 트랜잭션 2를 롤백
       → 트랜잭션 2의 S-Lock 해제

70ms   → 트랜잭션 1이 X-Lock 획득 ✓
       → UPDATE 성공
       → COMMIT
```

---

## 🔥 왜 S-Lock을 먼저 획득했는가?

### 가능성 1: REPEATABLE-READ의 잠금 메커니즘

MySQL의 기본 격리 수준은 **REPEATABLE-READ**입니다.

이 격리 수준에서 **SELECT ... FOR UPDATE**를 실행하면:
1. 먼저 **S-Lock**으로 읽기
2. 그 다음 **X-Lock**으로 업그레이드

이것은 MySQL의 **MVCC(Multi-Version Concurrency Control)** 때문입니다.

### 가능성 2: JPA의 Flush 타이밍

```java
@Transactional
public void someMethod(Long postId) {
    // 1. 비관적 락으로 조회
    CommunityPostEntity post = postRepository
        .findByPostIdAndIsDeletedFalseWithPessimisticLock(postId);

    // 2. 엔티티 수정
    post.increaseViewCount();

    // 3. 다른 작업들...
    // (여기서 SELECT ... FOR SHARE가 발생했을 수 있음)

    // 4. 커밋 시점에 UPDATE
    // → S-Lock을 X-Lock으로 업그레이드 시도
}
```

### 가능성 3: 락 호환성 테이블

```
         │  S-Lock  │  X-Lock
─────────┼──────────┼──────────
 S-Lock  │    ✓     │    ✗
 X-Lock  │    ✗     │    ✗
```

**중요:**
- 여러 트랜잭션이 **동시에 S-Lock 보유 가능** ✓
- S-Lock 보유 중인데 **X-Lock 획득 불가** ✗
- **업그레이드 시도 시 다른 S-Lock이 있으면 대기**

---

## 🔍 실제 코드에서 찾기

### 의심 지점 1: CommunityCountService
```java
@Transactional
public void increaseViewCount(Long postId) {
    // SELECT ... FOR UPDATE (X-Lock 시도)
    CommunityPostEntity post = postRepository
        .findByPostIdAndIsDeletedFalseWithPessimisticLock(postId);
    post.increaseViewCount();
}
```

**하지만** 로그를 보면 S-Lock을 보유하고 있습니다.

### 의심 지점 2: JPA의 내부 동작

**JPA + PESSIMISTIC_WRITE가 실제로 실행하는 SQL:**
```sql
-- Step 1: SELECT ... FOR UPDATE
SELECT * FROM community_posts WHERE post_id = ? FOR UPDATE;

-- Step 2 (커밋 시): UPDATE
UPDATE community_posts SET ... WHERE post_id = ?;
```

**하지만 REPEATABLE-READ에서:**
```sql
-- Step 1: 먼저 일관된 읽기를 위해 S-Lock
SELECT * FROM community_posts WHERE post_id = ? FOR SHARE;

-- Step 2: X-Lock으로 업그레이드 시도
SELECT * FROM community_posts WHERE post_id = ? FOR UPDATE;

-- Step 3: UPDATE 실행
UPDATE community_posts SET ... WHERE post_id = ?;
```

---

## 📌 데드락의 정확한 패턴

이것은 **"Lock Upgrade Deadlock"**이라고 불리는 클래식한 데드락 패턴입니다.

### 패턴 설명
```
1. 트랜잭션 A와 B가 동시에 같은 row에 S-Lock 획득 (가능!)
2. 둘 다 UPDATE를 시도 → X-Lock으로 업그레이드 필요
3. 트랜잭션 A: "B야, S-Lock 좀 풀어줘. 내가 X-Lock 받아야 해"
4. 트랜잭션 B: "A야, 너도 S-Lock 풀어줘. 나도 X-Lock 받아야 해"
5. 서로 대기 → DEADLOCK!
```

### 유명한 사례
```sql
-- 트랜잭션 A
BEGIN;
SELECT * FROM accounts WHERE id = 1 FOR SHARE;  -- S-Lock
-- ... 다른 작업 ...
UPDATE accounts SET balance = balance - 100 WHERE id = 1;  -- X-Lock 시도

-- 트랜잭션 B
BEGIN;
SELECT * FROM accounts WHERE id = 1 FOR SHARE;  -- S-Lock
-- ... 다른 작업 ...
UPDATE accounts SET balance = balance + 100 WHERE id = 1;  -- X-Lock 시도

→ DEADLOCK!
```

---

## 🎯 우리 코드에서 어디서 발생했는가?

### 추정되는 시나리오

k6 테스트에서 동시에 발생한 요청들:
```
POST /posts/1/views      (increaseViewCount)
POST /posts/1/comments   (createComment)
POST /posts/1/likes      (togglePostLike)
```

이들이 **모두 같은 post_id=1을 수정**하려고 시도했습니다.

### JPA + Hibernate의 동작 방식

```java
@Transactional
public void increaseViewCount(Long postId) {
    // 1. SELECT ... FOR UPDATE
    CommunityPostEntity post = postRepository
        .findByPostIdAndIsDeletedFalseWithPessimisticLock(postId);

    // 2. Dirty Checking으로 변경 감지
    post.increaseViewCount();

    // 3. 커밋 시점에 UPDATE
    // → 이 과정에서 S-Lock → X-Lock 업그레이드 발생 가능
}
```

**Hibernate가 내부적으로:**
1. 먼저 현재 값을 읽기 위해 S-Lock
2. 변경을 위해 X-Lock으로 업그레이드
3. 여러 트랜잭션이 동시에 이 과정을 거치면 데드락!

---

## 💡 해결 방법

### 방법 1: 처음부터 X-Lock만 사용
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM CommunityPostEntity p WHERE p.postId = :postId FOR UPDATE NOWAIT")
Optional<CommunityPostEntity> findByPostIdForUpdate(@Param("postId") Long postId);
```

**FOR UPDATE NOWAIT**:
- S-Lock을 거치지 않고 바로 X-Lock
- 락을 못 얻으면 즉시 에러 (대기하지 않음)

### 방법 2: Redis 카운터 (추천!)
```java
// S-Lock/X-Lock 없이 원자적 연산
redisTemplate.opsForHash().increment("post:1", "viewCount", 1);
```

### 방법 3: 낙관적 락
```java
@Entity
public class CommunityPostEntity {
    @Version
    private Long version;
}
```

### 방법 4: 락 순서 보장
```java
// 항상 같은 순서로 락 획득
// 예: user_id 작은 것 먼저, 큰 것 나중에
```

---

## 📚 핵심 학습 포인트

### 1. S-Lock과 X-Lock의 차이
- **S-Lock**: 읽기 전용, 여러 트랜잭션이 동시 보유 가능
- **X-Lock**: 쓰기용, 독점적 (다른 락과 호환 불가)

### 2. Lock Upgrade Deadlock
- 여러 트랜잭션이 S-Lock 보유
- 동시에 X-Lock으로 업그레이드 시도
- 서로를 기다리며 데드락

### 3. MySQL REPEATABLE-READ의 함정
- 일관된 읽기를 위해 내부적으로 S-Lock 사용
- UPDATE 시 X-Lock으로 업그레이드
- 동시성 높은 환경에서 데드락 위험

### 4. JPA + Hibernate의 숨겨진 동작
- Dirty Checking
- Flush 타이밍
- 내부 락 메커니즘

---

## 🔧 실전 팁

### 데드락 모니터링
```bash
# 실시간 데드락 확인
docker exec homesweet-db mysql -u root -prootpassword \
  -e "SHOW ENGINE INNODB STATUS\G" | grep -A 100 "LATEST DETECTED DEADLOCK"

# 데드락 발생 횟수 확인
docker exec homesweet-db mysql -u root -prootpassword \
  -e "SHOW STATUS LIKE 'Innodb_deadlock%';"
```

### 데드락 발생 시 재시도
```java
@Retryable(
    value = {CannotAcquireLockException.class, DeadlockLoserDataAccessException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 100, multiplier = 2)
)
public void increaseViewCount(Long postId) {
    // ...
}
```

### 락 대기 시간 설정
```sql
-- 50ms 이상 대기하지 않고 즉시 에러
SET innodb_lock_wait_timeout = 0.05;
```

---

## 🎓 결론

**데드락의 진짜 원인:**
1. ✅ 여러 요청이 같은 row (post_id=1)에 동시 접근
2. ✅ S-Lock을 먼저 획득 (REPEATABLE-READ 특성)
3. ✅ 동시에 X-Lock으로 업그레이드 시도
4. ✅ 순환 대기 발생 → DEADLOCK!

**당신의 이해가 정확했습니다:**
> "비관적 락(FOR UPDATE)은 다른 트랜잭션의 접근을 아예 막아버리는(직렬화) 방식입니다. 트래픽이 몰리면 병목 현상이 생기고, update 로직과 섞이면 바로 꼬입니다."

**추가로 발견한 사실:**
- FOR UPDATE도 내부적으로 S-Lock을 먼저 사용할 수 있음
- 여러 트랜잭션이 S-Lock을 동시 보유 가능
- X-Lock 업그레이드 시도가 데드락의 직접적 원인

**해결책:**
- Redis 카운터 (조회수, 좋아요 수)
- 낙관적 락 (댓글 수)
- 락 순서 보장
- FOR UPDATE NOWAIT
