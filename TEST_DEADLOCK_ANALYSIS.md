# 테스트 코드가 데드락을 발견하지 못한 이유

## 결론

**테스트 시나리오가 실제 데드락 상황을 재현하지 못했기 때문입니다.**

비관적 락이 천천히 실행되어서가 아닙니다.

---

## 현재 테스트 코드 분석

### 테스트 1: 조회수만 동시 실행

```java
@Test
@DisplayName("동시성 테스트 - 동시에 조회수 증가")
void concurrentViewCount() throws InterruptedException {
    int threadCount = 10;
    ExecutorService executorService = Executors.newFixedThreadPool(32);
    CountDownLatch latch = new CountDownLatch(threadCount);

    // 10개 스레드가 모두 조회수만 증가
    for (int i = 0; i < threadCount; i++) {
        executorService.submit(() -> {
            try {
                countService.increaseViewCount(testPost.getPostId());
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await();
    executorService.shutdown();

    CommunityPostEntity result = postRepository.findById(testPost.getPostId()).orElseThrow();
    assertThat(result.getViewCount()).isEqualTo(10);
}
```

#### 실행 순서

```
Thread 1: SELECT post WHERE id=1 FOR UPDATE (S Lock)
          → X Lock 획득
          → UPDATE view_count
          → COMMIT (락 해제)

Thread 2: SELECT post WHERE id=1 FOR UPDATE (대기)
          → Thread 1 완료 후 S Lock 획득
          → X Lock 획득
          → UPDATE view_count
          → COMMIT (락 해제)

...

Thread 10: 마찬가지로 순차 실행
```

#### 왜 데드락이 안 생기나

- 모든 스레드가 동일한 작업 (조회수 증가)
- 비관적 락으로 **직렬화**됨
- 한 스레드가 완료하면 다음 스레드 실행
- **순차 처리**되므로 데드락 없음

---

### 테스트 2: 좋아요만 동시 실행

```java
@Test
@DisplayName("동시성 테스트 - 동시에 좋아요 클릭")
void concurrentPostLike() throws InterruptedException {
    int threadCount = 10;
    ExecutorService executorService = Executors.newFixedThreadPool(32);
    CountDownLatch latch = new CountDownLatch(threadCount);

    // 10개 스레드가 모두 좋아요만 클릭
    for (int i = 0; i < threadCount; i++) {
        final int userIndex = i;
        executorService.submit(() -> {
            try {
                countService.togglePostLike(testPost.getPostId(), testUsers.get(userIndex).getId());
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await();
    executorService.shutdown();

    CommunityPostEntity result = postRepository.findById(testPost.getPostId()).orElseThrow();
    assertThat(result.getLikeCount()).isEqualTo(10);
}
```

#### 실행 순서

```
Thread 1: SELECT post WHERE id=1 FOR UPDATE (S Lock)
          → X Lock 획득
          → UPDATE like_count
          → COMMIT (락 해제)

Thread 2: SELECT post WHERE id=1 FOR UPDATE (대기)
          → Thread 1 완료 후 S Lock 획득
          → X Lock 획득
          → UPDATE like_count
          → COMMIT (락 해제)

...
```

#### 왜 데드락이 안 생기나

- 조회수 테스트와 동일
- 모든 스레드가 동일한 작업 (좋아요)
- **순차 처리**되므로 데드락 없음

---

## 실제 데드락 상황 (k6 테스트)

### k6 테스트 시나리오

```javascript
export function mixRequest() {
    const rand = Math.random();
    let res;

    // 33% 확률로 조회수 증가
    if (rand < 0.33) {
        res = http.post(`${API_BASE}/posts/${TARGET_POST_ID}/views`);
    }
    // 33% 확률로 좋아요 토글
    else if (rand < 0.66) {
        res = http.post(`${API_BASE}/posts/${TARGET_POST_ID}/likes`);
    }
    // 33% 확률로 댓글 작성
    else {
        const payload = JSON.stringify({ content: "데드락 테스트 댓글입니다." });
        res = http.post(`${API_BASE}/posts/${TARGET_POST_ID}/comments`, payload);
    }
}
```

### 실제 실행 순서

```
시간 0ms:
  Thread 1: 조회수 증가 시작
    → SELECT post WHERE id=1 FOR UPDATE (S Lock 획득)

시간 5ms:
  Thread 2: 좋아요 증가 시작
    → SELECT post WHERE id=1 FOR UPDATE (S Lock 획득) ← 가능! S Lock은 공유 가능

시간 10ms:
  Thread 1: UPDATE 시도
    → X Lock 요청 (대기 - Thread 2가 S Lock 보유 중)

시간 15ms:
  Thread 2: UPDATE 시도
    → X Lock 요청 (대기 - Thread 1이 S Lock 보유 중)

데드락 발생!
  - Thread 1: S Lock 보유, X Lock 대기 (Thread 2 때문에)
  - Thread 2: S Lock 보유, X Lock 대기 (Thread 1 때문에)
  → 순환 대기 → 데드락
```

---

## 왜 테스트에서는 안 생겼나

### 1. 동일한 작업만 테스트

```
테스트:
- 10개 스레드가 모두 조회수 증가
- 또는 10개 스레드가 모두 좋아요 증가

실제:
- 조회수 증가 (33%)
- 좋아요 증가 (33%)
- 댓글 작성 (33%)
- 세 가지가 섞여서 동시 실행
```

### 2. 비관적 락의 직렬화

```
테스트:
Thread 1: FOR UPDATE → X Lock → UPDATE → COMMIT
Thread 2: (대기) → FOR UPDATE → X Lock → UPDATE → COMMIT
Thread 3: (대기) → FOR UPDATE → X Lock → UPDATE → COMMIT
...

→ 순차 실행되므로 데드락 없음
```

### 3. S Lock 동시 획득 없음

```
데드락 조건:
1. 두 개 이상의 트랜잭션이 동시에 S Lock 획득
2. 둘 다 X Lock으로 업그레이드 시도

테스트에서:
- 한 번에 하나씩만 S Lock 획득
- S Lock 획득하면 바로 X Lock으로 업그레이드
- 다른 트랜잭션은 대기
→ 동시 S Lock 획득 없음
→ 데드락 없음
```

---

## 비관적 락이 천천히 실행되어서인가?

### 아니다

비관적 락의 속도와는 무관합니다.

```
느린 경우:
Thread 1: FOR UPDATE (1초 소요)
Thread 2: (1초 대기) → FOR UPDATE
→ 순차 실행, 데드락 없음

빠른 경우:
Thread 1: FOR UPDATE (0.001초 소요)
Thread 2: (0.001초 대기) → FOR UPDATE
→ 순차 실행, 데드락 없음

속도와 관계없이 순차 실행되므로 데드락 없음
```

### 데드락은 타이밍 문제

```
데드락 발생 조건:
1. 두 트랜잭션이 거의 동시에 시작 (0~10ms 차이)
2. 둘 다 S Lock 획득 (공유 가능하므로 둘 다 성공)
3. 둘 다 X Lock 요청 (서로 막힘)

테스트에서:
- 비관적 락이 직렬화
- 한 번에 하나씩만 실행
- 거의 동시 시작 불가능
```

---

## 올바른 테스트 코드

### 혼합 동시성 테스트 (데드락 재현)

```java
@Test
@DisplayName("동시성 테스트 - 조회/좋아요/댓글 혼합 (데드락 재현)")
void concurrentMixedOperations() throws InterruptedException {
    // given
    int threadCount = 30; // 더 많은 스레드
    ExecutorService executorService = Executors.newFixedThreadPool(30);
    CountDownLatch latch = new CountDownLatch(threadCount);
    AtomicInteger deadlockCount = new AtomicInteger(0);

    // when - 조회, 좋아요, 댓글을 무작위로 섞어서 실행
    for (int i = 0; i < threadCount; i++) {
        final int index = i;
        executorService.submit(() -> {
            try {
                int action = index % 3;

                if (action == 0) {
                    // 조회수 증가
                    countService.increaseViewCount(testPost.getPostId());
                } else if (action == 1) {
                    // 좋아요
                    countService.togglePostLike(testPost.getPostId(), testUsers.get(index % 10).getId());
                } else {
                    // 댓글 작성 (comment_count 증가)
                    CommunityCommentRequest request = new CommunityCommentRequest(
                        "테스트 댓글 " + index,
                        null
                    );
                    commentService.createComment(testPost.getPostId(), request, testUsers.get(index % 10).getId());
                }
            } catch (Exception e) {
                if (e.getMessage().contains("Deadlock")) {
                    deadlockCount.incrementAndGet();
                }
                System.err.println("Error: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await();
    executorService.shutdown();

    // then
    System.out.println("Deadlock count: " + deadlockCount.get());

    // 데드락이 발생하면 테스트 실패
    assertThat(deadlockCount.get()).isZero();
}
```

### 이 테스트의 차이점

```
1. 세 가지 작업을 동시 실행
   - 조회수 증가
   - 좋아요 토글
   - 댓글 작성

2. 더 많은 스레드 (30개)
   - 동시 실행 확률 증가

3. 무작위 타이밍
   - 실제 운영 환경과 유사

4. 데드락 감지
   - Exception 메시지에서 "Deadlock" 검출
```

### 예상 결과

```
비관적 락 사용 시:
- Deadlock count: 5~10건
- 테스트 실패

JPQL Atomic 사용 시:
- Deadlock count: 0건
- 테스트 통과
```

---

## 정리

### 테스트가 데드락을 못 잡은 이유

| 항목 | 기존 테스트 | 실제 상황 (k6) |
|------|-------------|----------------|
| 작업 종류 | 한 가지만 (조회 OR 좋아요) | 세 가지 혼합 (조회 + 좋아요 + 댓글) |
| 실행 방식 | 순차 실행 (비관적 락) | 동시 실행 (랜덤 타이밍) |
| S Lock 획득 | 한 번에 하나씩 | 동시에 여러 개 획득 |
| 데드락 발생 | 불가능 | 가능 |

### 비관적 락 속도와는 무관

```
느려도: 순차 실행 → 데드락 없음
빨라도: 순차 실행 → 데드락 없음

문제는 속도가 아니라 "동시 S Lock 획득"
```

### 데드락 발생 조건

```
필수 조건:
1. 두 개 이상의 트랜잭션이 거의 동시에 시작
2. 둘 다 S Lock을 획득 (공유 가능)
3. 둘 다 X Lock으로 업그레이드 시도 (서로 막힘)

테스트에서 재현하려면:
- 다양한 작업을 섞어서 실행
- 많은 스레드로 동시 실행
- 무작위 타이밍
```

---

## 개선된 테스트 전략

### 1. 단일 작업 테스트 (기존 유지)
```
목적: 각 기능의 정합성 확인
방법: 조회만, 좋아요만, 댓글만 따로 테스트
```

### 2. 혼합 동시성 테스트 (추가 필요)
```
목적: 데드락 발견
방법: 조회 + 좋아요 + 댓글을 섞어서 동시 실행
```

### 3. 부하 테스트 (k6)
```
목적: 실제 운영 환경 재현
방법: 수백 명이 동시에 무작위 작업
```

---

## 결론

테스트 코드가 데드락을 발견하지 못한 이유는 **비관적 락이 천천히 실행되어서가 아니라, 테스트 시나리오가 실제 데드락 상황을 재현하지 못했기 때문**입니다.

### 핵심 문제

```
기존 테스트: 동일한 작업만 순차 실행
실제 상황: 다양한 작업이 동시 실행

→ 데드락 조건 재현 실패
```

### 해결 방법

```
혼합 동시성 테스트 추가:
- 조회 + 좋아요 + 댓글을 섞어서 실행
- 많은 스레드로 동시 실행
- 데드락 발생 확인
```
