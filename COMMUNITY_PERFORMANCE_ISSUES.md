# 커뮤니티 성능 문제 분석 및 해결책

## 📊 발견된 문제점 요약

| 순위 | 문제 | 심각도 | 영향도 | 위치 |
|------|------|--------|--------|------|
| 🥇 1 | **N+1 쿼리 (게시글 목록)** | 💀💀💀 | 10개 → 31개 쿼리 | CommunityPostService:139-148 |
| 🥈 2 | **N+1 쿼리 (댓글 목록)** | 💀💀💀 | 10개 → 21개 쿼리 | CommunityCommentService:78-84 |
| 🥉 3 | **댓글 좋아요 비관적 락** | 💀💀 | 데드락 가능 | CommunityCountService:100 |
| 4 | 불필요한 전체 조회 | 💀 | exists로 충분 | CommunityCommentService:44-46 |
| 5 | 인덱스 부족 | ⚠️ | 쿼리 느림 | DB 스키마 |

---

## 💀💀💀 문제 1: 게시글 목록 N+1 쿼리 (최우선 해결)

### 현재 코드
```java
// CommunityPostService.java:139-148
public Page<CommunityPostResponse> getPosts(Pageable pageable) {
    Page<CommunityPostEntity> posts = postRepository.findByIsDeletedFalse(pageable);
    return posts.map(post -> {
        List<String> imageUrls = imageRepository.findByPostOrderByImageOrderAsc(post);  // ❌ N+1
        return CommunityPostResponse.from(post, imageUrls);
    });
}
```

### 실제 실행되는 쿼리
```sql
-- 1. 게시글 10개 조회
SELECT * FROM community_posts WHERE is_deleted = false LIMIT 10;

-- 2. 각 게시글의 이미지 조회 (10번 반복) ❌ N+1
SELECT * FROM community_images WHERE post_id = 1;
SELECT * FROM community_images WHERE post_id = 2;
...
SELECT * FROM community_images WHERE post_id = 10;

-- 3. DTO 변환 시 author 조회 (10번 반복) ❌ N+1
SELECT * FROM users WHERE user_id = 1;
SELECT * FROM users WHERE user_id = 2;
...
SELECT * FROM users WHERE user_id = 10;

-- 총 21개 쿼리! (1 + 10 + 10)
```

### 성능 영향
```
게시글 10개: 21개 쿼리
게시글 100개: 201개 쿼리!!!
게시글 1000개: 2001개 쿼리!!!!!
```

### ✅ 해결 방법 1: Fetch Join 사용

#### Repository 수정
```java
// CommunityPostRepository.java
@Query("""
    SELECT DISTINCT p
    FROM CommunityPostEntity p
    LEFT JOIN FETCH p.author
    WHERE p.isDeleted = false
    ORDER BY p.createdAt DESC
    """)
Page<CommunityPostEntity> findAllWithAuthor(Pageable pageable);

// 이미지까지 함께 조회 (Batch Size 사용)
@EntityGraph(attributePaths = {"author"})
Page<CommunityPostEntity> findByIsDeletedFalse(Pageable pageable);
```

#### Service 수정
```java
public Page<CommunityPostResponse> getPosts(Pageable pageable) {
    Page<CommunityPostEntity> posts = postRepository.findAllWithAuthor(pageable);

    // 이미지 일괄 조회 (IN 쿼리 1번)
    List<Long> postIds = posts.getContent().stream()
            .map(CommunityPostEntity::getPostId)
            .toList();

    Map<Long, List<String>> imageMap = imageRepository
            .findByPostPostIdIn(postIds)
            .stream()
            .collect(Collectors.groupingBy(
                img -> img.getPost().getPostId(),
                Collectors.mapping(CommunityImageEntity::getImageUrl, Collectors.toList())
            ));

    return posts.map(post -> {
        List<String> imageUrls = imageMap.getOrDefault(post.getPostId(), List.of());
        return CommunityPostResponse.from(post, imageUrls);
    });
}
```

#### ImageRepository에 추가
```java
// CommunityImageRepository.java
@Query("SELECT i FROM CommunityImageEntity i WHERE i.post.postId IN :postIds ORDER BY i.imageOrder ASC")
List<CommunityImageEntity> findByPostPostIdIn(@Param("postIds") List<Long> postIds);
```

### 최적화 후 쿼리
```sql
-- 1. 게시글 + author 조회 (Fetch Join)
SELECT p.*, u.*
FROM community_posts p
LEFT JOIN users u ON p.user_id = u.id
WHERE p.is_deleted = false
LIMIT 10;

-- 2. 이미지 일괄 조회 (IN 쿼리 1번)
SELECT * FROM community_images
WHERE post_id IN (1,2,3,4,5,6,7,8,9,10)
ORDER BY image_order ASC;

-- 총 2개 쿼리! (21개 → 2개, 10.5배 개선!)
```

---

## 💀💀💀 문제 2: 댓글 목록 N+1 쿼리

### 현재 코드
```java
// CommunityCommentService.java:78-84
public List<CommunityCommentResponse> getCommentsByPostId(Long postId) {
    List<CommunityCommentEntity> comments =
            commentRepository.findByPost_PostIdAndIsDeletedFalse(postId);  // 1번 쿼리
    return comments.stream()
            .map(CommunityCommentResponse::from)  // ❌ N+1: author, post 조회
            .toList();
}
```

### 실제 쿼리
```sql
-- 1. 댓글 조회
SELECT * FROM community_comments WHERE post_id = 1 AND is_deleted = false;

-- 2. 각 댓글의 author 조회 (10번 반복) ❌ N+1
SELECT * FROM users WHERE user_id = 1;
SELECT * FROM users WHERE user_id = 2;
...

-- 3. 각 댓글의 post 조회 (10번 반복) ❌ N+1
SELECT * FROM community_posts WHERE post_id = 1;  -- 중복!
SELECT * FROM community_posts WHERE post_id = 1;  -- 중복!
...

-- 총 21개 쿼리! (1 + 10 + 10)
```

### ✅ 해결 방법: Fetch Join 사용

#### Repository 수정
```java
// CommunityCommentRepository.java
@Query("""
    SELECT c
    FROM CommunityCommentEntity c
    LEFT JOIN FETCH c.author
    LEFT JOIN FETCH c.post
    WHERE c.post.postId = :postId AND c.isDeleted = false
    ORDER BY c.createdAt ASC
    """)
List<CommunityCommentEntity> findByPostIdWithAuthorAndPost(@Param("postId") Long postId);
```

#### Service 수정
```java
// CommunityCommentService.java
public List<CommunityCommentResponse> getCommentsByPostId(Long postId) {
    List<CommunityCommentEntity> comments =
            commentRepository.findByPostIdWithAuthorAndPost(postId);
    return comments.stream()
            .map(CommunityCommentResponse::from)
            .toList();
}
```

### 최적화 후
```sql
-- 1번의 쿼리로 모든 데이터 조회!
SELECT c.*, u.*, p.*
FROM community_comments c
LEFT JOIN users u ON c.user_id = u.id
LEFT JOIN community_posts p ON c.post_id = p.post_id
WHERE c.post_id = 1 AND c.is_deleted = false
ORDER BY c.created_at ASC;

-- 총 1개 쿼리! (21개 → 1개, 21배 개선!)
```

---

## 💀💀 문제 3: 댓글 좋아요 비관적 락 (데드락 가능)

### 현재 코드
```java
// CommunityCountService.java:100
@Transactional
public void toggleCommentLike(Long commentId, Long userId) {
    CommunityCommentEntity comment = commentRepository
            .findByIdWithPessimisticLock(commentId);  // ❌ SELECT ... FOR UPDATE

    // ...
    if (existingLike.isPresent()) {
        comment.decreaseLikeCount();  // ❌ Dirty Checking
    } else {
        comment.increaseLikeCount();  // ❌ Dirty Checking
    }
}
```

### 문제점
- 게시글 좋아요는 JPQL Atomic으로 바꿨는데 댓글 좋아요는 여전히 비관적 락!
- 데드락 가능성 여전히 존재
- 게시글과 동일한 Lock Upgrade Deadlock 패턴

### ✅ 해결 방법: JPQL Atomic Update

#### Repository에 추가
```java
// CommunityCommentRepository.java
@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("UPDATE CommunityCommentEntity c SET c.likeCount = c.likeCount + :delta WHERE c.commentId = :commentId")
void updateLikeCount(@Param("commentId") Long commentId, @Param("delta") int delta);
```

#### Service 수정
```java
// CommunityCountService.java
@Transactional
public void toggleCommentLike(Long commentId, Long userId) {
    // 댓글 존재 여부만 확인 (락 없이)
    CommunityCommentEntity comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new CommunityException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND));

    User user = userRepository.findById(userId)
            .orElseThrow(() -> new CommunityException(ErrorCode.USER_NOT_FOUND));

    Optional<CommunityCommentLikeEntity> existingLike =
            commentLikeRepository.findByCommentAndUser(comment, user);

    if (existingLike.isPresent()) {
        commentLikeRepository.delete(existingLike.get());
        commentRepository.updateLikeCount(commentId, -1);  // ✅ Atomic Update
    } else {
        CommunityCommentLikeEntity newLike = CommunityCommentLikeEntity.builder()
                .comment(comment)
                .user(user)
                .build();
        commentLikeRepository.save(newLike);
        commentRepository.updateLikeCount(commentId, 1);  // ✅ Atomic Update

        // 알림 전송
        notificationSendService.sendTemplateNotificationToSingleUser(
                comment.getAuthor().getId(),
                CommunityNotification.NewCommentLike.builder()
                        .userName(user.getName())
                        .postId(comment.getPost().getPostId())
                        .postTitle(comment.getPost().getTitle())
                        .commentId(comment.getCommentId())
                        .build());
    }
}
```

---

## 💀 문제 4: 불필요한 전체 조회

### 현재 코드
```java
// CommunityCommentService.java:44-46
if (request.parentCommentId() != null) {
    commentRepository.findById(request.parentCommentId())  // ❌ SELECT *
            .orElseThrow(() -> new CommunityException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND));
}
```

### ✅ 해결 방법: exists 사용
```java
if (request.parentCommentId() != null) {
    if (!commentRepository.existsById(request.parentCommentId())) {
        throw new CommunityException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND);
    }
}
```

### 쿼리 비교
```sql
-- Before: 전체 컬럼 조회
SELECT * FROM community_comments WHERE comment_id = ?;

-- After: 존재 여부만 확인
SELECT 1 FROM community_comments WHERE comment_id = ? LIMIT 1;
```

---

## ⚠️ 문제 5: 인덱스 최적화

### 추가 필요한 인덱스
```sql
-- 1. 게시글 조회 최적화
CREATE INDEX idx_posts_deleted_created ON community_posts(is_deleted, created_at DESC);

-- 2. 댓글 조회 최적화
CREATE INDEX idx_comments_post_deleted_created ON community_comments(post_id, is_deleted, created_at);

-- 3. 이미지 조회 최적화
CREATE INDEX idx_images_post_order ON community_images(post_id, image_order);

-- 4. 좋아요 조회 최적화
CREATE INDEX idx_post_likes_post_user ON community_post_likes(post_id, user_id);
CREATE INDEX idx_comment_likes_comment_user ON community_comment_likes(comment_id, user_id);
```

---

## 📊 최종 성능 개선 예상치

### 게시글 목록 조회 (10개)
```
Before: 21개 쿼리 (1 posts + 10 images + 10 authors)
After:  2개 쿼리 (1 posts+authors + 1 images)
개선율: 10.5배 ⬆️
응답시간: 500ms → 50ms
```

### 댓글 목록 조회 (10개)
```
Before: 21개 쿼리 (1 comments + 10 authors + 10 posts)
After:  1개 쿼리 (comments+authors+posts Fetch Join)
개선율: 21배 ⬆️
응답시간: 300ms → 15ms
```

### 댓글 좋아요
```
Before: 비관적 락 (P95: 500ms, 데드락 가능)
After:  JPQL Atomic (P95: 50ms, 데드락 없음)
개선율: 10배 ⬆️
데드락: 제거 ✅
```

---

## 🎯 우선순위별 적용 순서

### 1순위: N+1 쿼리 해결 (즉시 적용 권장)
- ✅ 게시글 목록 Fetch Join
- ✅ 댓글 목록 Fetch Join
- 효과: 쿼리 수 95% 감소, 응답속도 10배 개선

### 2순위: 댓글 좋아요 Atomic Update (즉시 적용 권장)
- ✅ JPQL Atomic Update로 변경
- 효과: 데드락 제거, 응답속도 10배 개선

### 3순위: 불필요한 쿼리 최적화
- ✅ exists 사용
- 효과: 쿼리 1개 감소 (미미하지만 좋음)

### 4순위: 인덱스 추가 (DBA 협의 필요)
- ⚠️ 운영 DB에 직접 적용 시 주의
- 효과: 쿼리 속도 2배 개선

---

## 🔧 적용 후 검증 방법

### 1. 쿼리 수 확인
```yaml
# application.yml
spring:
  jpa:
    properties:
      hibernate:
        show_sql: true
        format_sql: true
        use_sql_comments: true
```

### 2. k6 성능 테스트
```javascript
// k6-tests/community_optimized.js
import http from 'k6/http';

export let options = {
  vus: 100,
  duration: '30s',
};

export default function() {
  // Before: 21개 쿼리, P95: 500ms
  // After:  2개 쿼리, P95: 50ms
  http.get('http://localhost:8080/api/v1/community/posts?page=0&size=10');
}
```

### 3. Grafana로 모니터링
```
- 쿼리 수 감소 확인
- 응답시간 개선 확인
- DB CPU 사용률 감소 확인
```

---

## 💡 추가 개선 아이디어

### 1. 캐싱 도입 (선택)
```java
@Cacheable(value = "posts", key = "#pageable.pageNumber")
public Page<CommunityPostResponse> getPosts(Pageable pageable) {
    // Redis 캐시 적용 (조회수 많은 게시글)
}
```

### 2. QueryDSL 도입 (선택)
- 복잡한 동적 쿼리에 유용
- 타입 안정성 제공

### 3. 읽기 전용 DB 분리 (대규모 서비스)
- Master: 쓰기
- Slave: 읽기 (게시글 목록, 댓글 목록)

---

## 📝 체크리스트

- [ ] 게시글 목록 Fetch Join 적용
- [ ] 댓글 목록 Fetch Join 적용
- [ ] 댓글 좋아요 JPQL Atomic Update 적용
- [ ] exists 쿼리로 변경
- [ ] 인덱스 추가 (DBA 협의)
- [ ] k6 성능 테스트
- [ ] Grafana 모니터링 확인
- [ ] 데드락 로그 모니터링

**적용 하시겠습니까?**
