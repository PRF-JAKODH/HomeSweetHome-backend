# DB/시스템 튜닝 가이드 (Redis 전에 먼저!)

## 🎯 핵심 원칙

> **"새로운 인프라를 추가하기 전에, 기존 인프라를 최대한 활용하라"**

Redis는 마지막 5% 개선을 위한 것입니다. 먼저 DB와 시스템 튜닝으로 95%를 끌어올릴 수 있습니다.

---

## 📊 최적화 우선순위

| 순위 | 최적화 | 난이도 | 비용 | 예상 개선 | ROI |
|------|--------|--------|------|-----------|-----|
| 1️⃣ | **복합 인덱스 추가** | ⭐ | 0원 | 2~10배 | ⭐⭐⭐⭐⭐ |
| 2️⃣ | **N+1 쿼리 제거** | ⭐⭐ | 0원 | 10~20배 | ⭐⭐⭐⭐⭐ |
| 3️⃣ | **Connection Pool 튜닝** | ⭐ | 0원 | 1.5배 | ⭐⭐⭐⭐ |
| 4️⃣ | **InnoDB Buffer Pool** | ⭐ | 0원 | 2~3배 | ⭐⭐⭐⭐ |
| 5️⃣ | **하드웨어 스케일업** | ⭐ | $50/월 | 2배 | ⭐⭐⭐ |
| 6️⃣ | **커널 TCP 설정** | ⭐⭐⭐ | 0원 | 1.2~1.5배 | ⭐⭐ |
| 7️⃣ | **Redis** | ⭐⭐⭐⭐ | $100/월 | 1.1배 | ⭐ |

---

## 🥇 1순위: 복합 인덱스 추가 (10배 개선 가능)

### 현재 상태 확인
```sql
-- 게시글 목록 조회 EXPLAIN
EXPLAIN SELECT *
FROM community_posts
WHERE is_deleted = false
ORDER BY created_at DESC
LIMIT 10;
```

**예상 결과 (인덱스 없을 때):**
```
type: ALL          -- ❌ Full Table Scan
rows: 227          -- ❌ 전체 읽음
Extra: Using filesort  -- ❌ 정렬 느림
```

### 복합 인덱스 추가
```sql
-- 1. 게시글 목록 최적화
CREATE INDEX idx_deleted_created
ON community_posts(is_deleted, created_at DESC);

-- 2. 댓글 목록 최적화
CREATE INDEX idx_post_deleted_created
ON community_comments(post_id, is_deleted, created_at ASC);

-- 3. 이미지 조회 최적화
CREATE INDEX idx_post_order
ON community_images(post_id, image_order ASC);

-- 4. 좋아요 조회 최적화
CREATE INDEX idx_post_user
ON community_post_likes(post_id, user_id);

CREATE INDEX idx_comment_user
ON community_comment_likes(comment_id, user_id);
```

### 효과 확인
```sql
-- 다시 EXPLAIN 실행
EXPLAIN SELECT *
FROM community_posts
WHERE is_deleted = false
ORDER BY created_at DESC
LIMIT 10;
```

**개선된 결과:**
```
type: ref          -- ✅ 인덱스 사용
rows: 10           -- ✅ 10개만 읽음 (227개 → 10개, 22배 개선!)
Extra: NULL        -- ✅ filesort 사라짐
```

**예상 개선: 2~10배** ⬆️

---

## 🥈 2순위: N+1 쿼리 제거 (20배 개선 가능)

### 문제 확인
```yaml
# application.yml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

**로그 확인:**
```
게시글 10개 조회 → 21개 쿼리 발생! ❌
1. SELECT * FROM community_posts  (1개)
2. SELECT * FROM users WHERE id = ? (10개, N+1)
3. SELECT * FROM community_images WHERE post_id = ? (10개, N+1)
```

### 해결 방법

#### 1. 게시글 목록 Fetch Join
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
```

#### 2. 이미지 IN 쿼리
```java
// CommunityImageRepository.java
@Query("SELECT i FROM CommunityImageEntity i WHERE i.post.postId IN :postIds ORDER BY i.imageOrder ASC")
List<CommunityImageEntity> findByPostPostIdIn(@Param("postIds") List<Long> postIds);

// CommunityPostService.java
public Page<CommunityPostResponse> getPosts(Pageable pageable) {
    Page<CommunityPostEntity> posts = postRepository.findAllWithAuthor(pageable);

    // 이미지 일괄 조회
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

**효과:**
```
Before: 21개 쿼리
After:  2개 쿼리 (1 posts+author + 1 images)

개선율: 10.5배 ⬆️
```

**예상 개선: 10~20배** ⬆️

---

## 🥉 3순위: Connection Pool 튜닝 (1.5배 개선)

### 현재 설정 확인
```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10  # ❌ 너무 작음!
      minimum-idle: 10
      connection-timeout: 30000
```

### 최적화 설정
```yaml
spring:
  datasource:
    hikari:
      # 공식: (core_count * 2) + effective_spindle_count
      # 예: CPU 4코어, SSD 1개 = 4*2 + 1 = 9 → 10~20으로 설정
      maximum-pool-size: 20         # ✅ 증가
      minimum-idle: 10              # ✅ 유지
      connection-timeout: 30000     # 30초
      idle-timeout: 600000          # 10분
      max-lifetime: 1800000         # 30분

      # 성능 모니터링
      leak-detection-threshold: 60000  # 1분 이상 열려있으면 경고
```

### 모니터링
```java
@RestController
public class MonitoringController {
    @Autowired
    private HikariDataSource dataSource;

    @GetMapping("/actuator/hikari")
    public Map<String, Object> hikariStats() {
        HikariPoolMXBean poolMXBean = dataSource.getHikariPoolMXBean();
        return Map.of(
            "active", poolMXBean.getActiveConnections(),
            "idle", poolMXBean.getIdleConnections(),
            "total", poolMXBean.getTotalConnections(),
            "waiting", poolMXBean.getThreadsAwaitingConnection()
        );
    }
}
```

**예상 개선: 1.5배** ⬆️

---

## 4️⃣ InnoDB Buffer Pool 튜닝 (2~3배 개선)

### 현재 설정 확인
```sql
SHOW VARIABLES LIKE 'innodb_buffer_pool_size';
```

**기본값:** 128MB (너무 작음!)

### 최적 설정
```ini
# MySQL 설정 파일 (my.cnf 또는 my.ini)
[mysqld]
# Buffer Pool = RAM의 70~80%
# 예: RAM 8GB → 6GB
innodb_buffer_pool_size = 6G

# Buffer Pool 인스턴스 (CPU 코어 수)
innodb_buffer_pool_instances = 4

# 로그 파일 크기 (Buffer Pool의 25%)
innodb_log_file_size = 1.5G

# Flush 방법 (SSD 환경)
innodb_flush_method = O_DIRECT

# Flush 빈도 조정
innodb_flush_log_at_trx_commit = 2  # 1초마다 (성능 우선)
# 주의: 1 = 안전, 2 = 빠름 (1초 데이터 유실 가능)
```

### Docker 환경 적용
```yaml
# docker-compose.dev.yml
services:
  db:
    image: mysql:8.0
    command:
      - --innodb-buffer-pool-size=2G
      - --innodb-buffer-pool-instances=2
      - --innodb-log-file-size=512M
      - --innodb-flush-method=O_DIRECT
      - --innodb-flush-log-at-trx-commit=2
```

### 효과 확인
```sql
-- Buffer Pool 히트율 확인 (99% 이상이 목표)
SELECT
  (1 - (Innodb_buffer_pool_reads / Innodb_buffer_pool_read_requests)) * 100
  AS hit_rate
FROM
  (SELECT
    VARIABLE_VALUE AS Innodb_buffer_pool_reads
   FROM performance_schema.global_status
   WHERE VARIABLE_NAME = 'Innodb_buffer_pool_reads') reads,
  (SELECT
    VARIABLE_VALUE AS Innodb_buffer_pool_read_requests
   FROM performance_schema.global_status
   WHERE VARIABLE_NAME = 'Innodb_buffer_pool_read_requests') requests;
```

**예상 개선: 2~3배** ⬆️

---

## 5️⃣ 하드웨어 스케일업 (2배 개선)

### 수직 확장 (Scale Up)

#### CPU 증가
```
현재: 2 vCPU
권장: 4 vCPU

효과: CPU 집약적 쿼리 2배 빨라짐
비용: +$25/월 (AWS t3.medium → t3.large)
```

#### RAM 증가
```
현재: 4GB RAM
권장: 8GB RAM

효과: Buffer Pool 2배 → 디스크 I/O 50% 감소
비용: +$25/월
```

#### SSD 사용
```
HDD → SSD: I/O 10배 개선
비용: +$10/월
```

**총 예상 개선: 2배** ⬆️
**총 추가 비용: $50~60/월**

---

## 6️⃣ 커널 튜닝 (1.2~1.5배 개선)

### Linux 커널 설정

#### 1. TCP 설정 최적화
```bash
# /etc/sysctl.conf
# TCP 연결 큐 크기 증가
net.core.somaxconn = 4096
net.ipv4.tcp_max_syn_backlog = 8192

# TIME_WAIT 소켓 재사용
net.ipv4.tcp_tw_reuse = 1

# TCP 버퍼 크기 증가
net.core.rmem_max = 16777216
net.core.wmem_max = 16777216
net.ipv4.tcp_rmem = 4096 87380 16777216
net.ipv4.tcp_wmem = 4096 65536 16777216

# 적용
sudo sysctl -p
```

#### 2. File Descriptor 증가
```bash
# /etc/security/limits.conf
mysql soft nofile 65535
mysql hard nofile 65535

# 확인
ulimit -n
```

#### 3. Swap 비활성화 (DB 서버)
```bash
# Swap은 DB 성능을 크게 저하시킴
sudo swapoff -a

# /etc/fstab에서 swap 라인 주석 처리
```

### Docker 환경 설정
```yaml
# docker-compose.dev.yml
services:
  db:
    image: mysql:8.0
    ulimits:
      nofile:
        soft: 65535
        hard: 65535
    sysctls:
      - net.core.somaxconn=4096
```

**예상 개선: 1.2~1.5배** ⬆️

---

## 📊 전체 최적화 누적 효과

### 시나리오 1: 모든 최적화 적용
```
기준 성능: 2,000 req/s (JPQL Atomic)

1. 복합 인덱스:        2,000 × 2    =  4,000 req/s
2. N+1 제거:          4,000 × 3    = 12,000 req/s
3. Connection Pool:  12,000 × 1.5  = 18,000 req/s
4. Buffer Pool:      18,000 × 2    = 36,000 req/s
5. 하드웨어:         36,000 × 2    = 72,000 req/s (!!)
6. 커널 튜닝:        72,000 × 1.3  = 93,600 req/s (!!!)

총 개선: 46.8배 ⬆️⬆️⬆️
```

### 시나리오 2: 비용 0원 최적화만
```
기준 성능: 2,000 req/s

1. 복합 인덱스:        2,000 × 2    =  4,000 req/s
2. N+1 제거:          4,000 × 3    = 12,000 req/s
3. Connection Pool:  12,000 × 1.5  = 18,000 req/s
4. Buffer Pool:      18,000 × 2    = 36,000 req/s
5. 커널 튜닝:        36,000 × 1.3  = 46,800 req/s

총 개선: 23.4배 ⬆️⬆️
총 비용: 0원
```

**결론: 돈 안 써도 23배 개선 가능!**

---

## 🎯 단계별 적용 가이드

### Week 1: 인덱스 최적화 (가장 쉽고 효과 큰 것)
```sql
-- 5분이면 끝
CREATE INDEX idx_deleted_created ON community_posts(is_deleted, created_at DESC);
CREATE INDEX idx_post_deleted_created ON community_comments(post_id, is_deleted, created_at ASC);
CREATE INDEX idx_post_order ON community_images(post_id, image_order ASC);
CREATE INDEX idx_post_user ON community_post_likes(post_id, user_id);
CREATE INDEX idx_comment_user ON community_comment_likes(comment_id, user_id);

-- 효과 측정 (1주일 모니터링)
```

### Week 2: N+1 쿼리 제거
```java
// Fetch Join 적용 (반나절)
// 효과 측정 (1주일 모니터링)
```

### Week 3: Connection Pool & Buffer Pool
```yaml
# 설정 변경 (1시간)
# 효과 측정 (1주일 모니터링)
```

### Week 4: 커널 튜닝 (선택)
```bash
# 운영 환경에서 신중하게
# 효과 측정
```

### Week 5: 평가
```
목표 TPS 도달했나?
- YES: Redis 불필요! ✅
- NO: 하드웨어 스케일업 or Redis 검토
```

---

## ⚠️ 주의사항

### 1. 운영 DB에 적용 시
```
✅ 인덱스 추가: 온라인 DDL 사용
CREATE INDEX ... ALGORITHM=INPLACE, LOCK=NONE;

✅ 설정 변경: 점진적 적용
- 먼저 개발 환경 테스트
- staging 환경 1주일 검증
- 운영 환경 적용

❌ 급격한 변경 금지
- Buffer Pool 크기 2배씩 서서히
- Connection Pool 10 → 15 → 20
```

### 2. 모니터링 필수
```yaml
모니터링 지표:
- TPS (초당 요청)
- P50, P95, P99 응답시간
- DB CPU 사용률
- DB Memory 사용률
- Connection Pool 사용률
- Buffer Pool 히트율
- 쿼리 실행 시간

도구:
- Grafana + Prometheus
- MySQL Slow Query Log
- Spring Boot Actuator
```

### 3. 백업 필수
```bash
# 설정 변경 전 백업
mysqldump -u root -p homesweet > backup_$(date +%Y%m%d).sql

# 커널 설정 백업
cp /etc/sysctl.conf /etc/sysctl.conf.backup
```

---

## 📈 성능 측정 방법

### k6 벤치마크
```javascript
// k6-tests/performance_test.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '1m', target: 100 },   // 100 req/s
    { duration: '1m', target: 500 },   // 500 req/s
    { duration: '1m', target: 1000 },  // 1000 req/s
    { duration: '1m', target: 2000 },  // 2000 req/s
  ],
  thresholds: {
    http_req_duration: ['p(95)<100'],  // P95 < 100ms
  },
};

export default function() {
  http.get('http://localhost:8080/api/v1/community/posts?page=0&size=10');
  sleep(0.1);
}
```

### 결과 비교
```
Before (인덱스 없음):
- TPS: 200
- P95: 500ms
- CPU: 80%

After (인덱스 추가):
- TPS: 2,000 (10배!)
- P95: 50ms (10배 빠름!)
- CPU: 20% (4배 감소!)
```

---

## 🎓 결론

### Redis가 필요한 경우
```
조건:
✅ 모든 DB/시스템 튜닝 완료
✅ 그래도 TPS > 10,000 필요
✅ DB CPU > 80%
✅ P95 > 200ms

→ 이때 Redis 도입!
```

### Redis가 불필요한 경우 (대부분)
```
현재 상황:
- TPS < 1,000
- DB 튜닝 안 함
- N+1 쿼리 존재
- 인덱스 없음

→ DB 튜닝만으로 충분!
```

**"새 기술을 도입하기 전에, 기존 기술을 최대한 활용하라"** 🚀
