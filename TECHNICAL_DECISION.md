# 커뮤니티 카운터 업데이트 기술 의사결정

## 📌 결정 사항

**JPQL Atomic Update 방식 채택**

## 🎯 의사결정 배경

### 1. 현재 프로젝트 상태
- **단계**: MVP (Minimum Viable Product)
- **예상 트래픽**: 초당 100건 미만
- **인프라**: 단일 MySQL 서버
- **팀 규모**: 소규모
- **목표**: 빠른 출시, 안정성 확보

---

## 🤔 고려한 옵션

| 방식 | 구현 복잡도 | 성능 | 인프라 비용 | 유지보수 | MVP 적합성 |
|------|-------------|------|-------------|----------|------------|
| **JPQL Atomic** | ⭐ (매우 쉬움) | ⭐⭐⭐ (2000 req/s) | ⭐⭐⭐ (추가 없음) | ⭐⭐⭐ | ✅ **최적** |
| **Redis** | ⭐⭐⭐ (복잡) | ⭐⭐⭐⭐⭐ (50000 req/s) | ⭐ (Redis 필요) | ⭐⭐ | ⚠️ **과함** |
| **메시지큐** | ⭐⭐⭐⭐ (매우 복잡) | ⭐⭐⭐ (비동기) | ⭐ (Kafka/RabbitMQ 필요) | ⭐ | ❌ **부적합** |

---

## ✅ JPQL Atomic Update를 선택한 이유

### 1. **성능이 충분함**
```
현재 필요 성능: 초당 100건
JPQL 처리 능력: 초당 2,000건
여유율: 20배

결론: 트래픽이 20배 증가해도 문제없음
```

### 2. **구현 복잡도가 낮음**
```java
// JPQL: 3줄로 해결
@Modifying
@Query("UPDATE CommunityPostEntity p SET p.viewCount = p.viewCount + 1 WHERE p.postId = :postId")
void incrementViewCount(@Param("postId") Long postId);

// Redis: 30줄 + 동기화 배치 + 장애 처리
@Service
public class RedisCountService {
    // Redis 카운터 증가
    // Redis 장애 시 Fallback
    // DB 동기화 배치 (1분마다)
    // 데이터 정합성 검증
    // ...
}
```

**개발 시간 비교:**
- JPQL: 1시간
- Redis: 1주일 (동기화 로직, 테스트, 장애 처리)

### 3. **인프라 비용 절감**
```
JPQL 방식:
- MySQL만 사용 (기존 인프라)
- 추가 비용: 0원

Redis 방식:
- Redis 서버 추가 필요
- AWS ElastiCache: 월 $50~100
- 관리 공수 증가
- 장애 포인트 +1
```

### 4. **데드락 해결 완료**
```sql
-- Before: JPA Dirty Checking (데드락 발생)
SELECT * FROM community_posts WHERE post_id = 1 FOR UPDATE;
UPDATE community_posts SET view_count = 100, like_count = 50, ... WHERE post_id = 1;

-- After: JPQL Atomic (데드락 없음)
UPDATE community_posts SET view_count = view_count + 1 WHERE post_id = 1;
```

**k6 테스트 결과:**
- Before: 데드락 빈번 발생 💀
- After: 데드락 0건 ✅

### 5. **차후 확장 가능**
```java
// 1단계: JPQL (현재)
@Transactional
public void increaseViewCount(Long postId) {
    postRepository.incrementViewCount(postId);
}

// 2단계: Redis (트래픽 증가 시)
@Transactional
public void increaseViewCount(Long postId) {
    // 인터페이스 동일, 구현만 변경
    redisCountService.incrementViewCount(postId);
}
```

**마이그레이션 전략:**
1. 인터페이스 변경 없음
2. 점진적 전환 (조회수만 Redis → 전체 Redis)
3. A/B 테스트 가능

---

## ⚠️ Redis를 당장 도입하지 않는 이유

### 1. **과도한 성능**
```
Redis 성능: 초당 50,000건
현재 필요: 초당 100건

→ 500배 오버스펙 (비용 대비 효과 없음)
```

### 2. **복잡도 증가**
```
추가해야 할 것:
✅ Redis 서버 설치 및 설정
✅ Redis 장애 시 Fallback 로직
✅ DB 동기화 배치 (1분마다)
✅ 정합성 검증 (Redis vs DB)
✅ Redis 데이터 유실 시 복구
✅ Redis 모니터링 설정
✅ Redis 백업 전략

→ MVP 단계에서 과함
```

### 3. **새로운 장애 포인트**
```
JPQL 방식: MySQL 장애만 관리
Redis 방식: MySQL 장애 + Redis 장애 + 동기화 실패

장애 확률 = 2배 증가
```

### 4. **실시간 정확성 문제**
```
JPQL: 실시간 정확 (항상 DB와 일치)
Redis: 최대 1분 지연 (배치 동기화)

예시:
- 사용자가 조회수를 봄 → Redis: 100
- DB를 직접 조회 → MySQL: 95 (동기화 전)
→ 데이터 불일치
```

---

## ❌ 메시지큐를 선택하지 않는 이유

### 1. **실시간성 필수**
```
요구사항: 사용자가 좋아요 누르면 즉시 카운트 증가
메시지큐: 비동기 처리 (수 초 ~ 수십 초 지연)

→ UX 나쁨
```

### 2. **순서 보장 문제**
```
시나리오:
1. 좋아요 추가 (메시지큐)
2. 좋아요 취소 (메시지큐)

문제: 순서가 뒤바뀌면?
→ 취소 먼저 처리 → 추가 처리 → 카운트 증가 (잘못됨!)
```

### 3. **복잡도 극대화**
```
필요한 것:
✅ Kafka/RabbitMQ 클러스터 구축
✅ Producer/Consumer 구현
✅ Dead Letter Queue 처리
✅ 순서 보장 로직
✅ 멱등성 처리 (중복 방지)
✅ 장애 복구 로직

→ 카운터 업데이트에는 과함
```

### 4. **메시지큐는 다른 용도에 더 적합**
```
✅ 적합한 용도:
- 알림 발송 (비동기 OK)
- 이메일 발송 (비동기 OK)
- 로그 수집 (순서 무관)
- 이벤트 전파 (최종 일관성)

❌ 부적합한 용도:
- 카운터 업데이트 (실시간 필수)
- 잔액 차감 (즉시 반영)
- 재고 차감 (정확성 필수)
```

---

## 📊 트래픽별 권장 방식

| 트래픽 (req/s) | 권장 방식 | 이유 |
|----------------|-----------|------|
| ~ 100 | **JPQL Atomic** | 충분한 성능, 간단함 |
| 100 ~ 1,000 | **JPQL Atomic** | 여전히 충분 |
| 1,000 ~ 10,000 | **Redis (조회수만)** | 조회수만 Redis로 분리 |
| 10,000 ~ | **Redis (전체)** | 모든 카운터 Redis |

**현재 예상 트래픽: 초당 50~100건**
→ JPQL Atomic이 최적!

---

## 🚀 확장 전략 (단계적 최적화)

### Phase 1: 코드 최적화 (현재) ✅
```
방식: JPQL Atomic Update
목표: 데드락 해결, 안정적 서비스
성능: 2,000 req/s
비용: 0원
```

### Phase 2: 트래픽 모니터링 (출시 후)
```
모니터링 지표:
- 초당 요청 수 (TPS)
- DB CPU 사용률
- P95 응답 속도
- 쿼리 실행 시간

임계치:
- TPS > 1,000
- DB CPU > 70%
- P95 > 100ms

→ 임계치 도달 시 Phase 3로 (Redis 아님!)
```

### Phase 3: DB/시스템 튜닝 (Redis 전에 먼저!) 🎯
```
우선순위:
1. 인덱스 최적화 (복합 인덱스 추가) → 2~10배 개선
2. N+1 쿼리 제거 (Fetch Join) → 10~20배 개선
3. MySQL 설정 튜닝 → 1.5~3배 개선
   - InnoDB buffer pool 증가
   - Connection pool 최적화
4. 하드웨어 스케일업 (CPU/RAM) → 2배 개선
5. 커널 튜닝 → 1.2~2배 개선
   - TCP 설정 최적화
   - File descriptor 증가

총 예상 개선: 최대 60배 🚀
추가 비용: $0 (하드웨어 제외)
복잡도: 낮음 (기존 시스템)
```

### Phase 4: Redis 도입 (Phase 3 이후에도 부족할 때)
```
조건:
- Phase 3 튜닝을 모두 적용했는데도
- TPS > 10,000 또는
- DB CPU > 80% 또는
- P95 > 200ms

우선순위:
1. 조회수만 Redis로 (가장 빈번)
2. 좋아요 Redis로
3. 댓글수는 JPQL 유지 (빈도 낮음)

점진적 전환:
- A/B 테스트
- 1주일 모니터링
- 문제 없으면 전체 적용
```

### Phase 5: 메시지큐 검토 (대규모 서비스)
```
고려 시점:
- DAU 100만 이상
- TPS 50,000 이상
- 알림, 이벤트 처리 필요

용도:
- 알림 발송 (메시지큐)
- 카운터는 여전히 Redis
```

---

## 💡 왜 Redis 전에 DB 튜닝을 먼저 해야 할까?

### 1. **비용 효율성**
```
DB 튜닝:
- 인덱스 추가: 비용 0원, 성능 10배 개선
- MySQL 설정: 비용 0원, 성능 3배 개선
- 총: 비용 0원, 성능 30배 개선

Redis:
- 개발 비용: 1주일
- 인프라 비용: 월 $50~100
- 복잡도: 높음
- 성능: 25배 개선 (2,000 → 50,000)

결론: DB 튜닝이 먼저!
```

### 2. **문제의 근본 원인**
```
현재 성능 문제 (예상):
✅ N+1 쿼리: 21개 쿼리 → 2개 쿼리 (10배 개선)
✅ 인덱스 없음: Full Table Scan (20배 개선)
✅ Connection pool 부족: 대기 시간 발생

→ 이걸 먼저 해결하면 Redis 불필요!
```

### 3. **단계적 접근**
```
1단계: 애플리케이션 레벨 (JPQL) ✅ 완료
2단계: 데이터베이스 레벨 (인덱스, 쿼리) ← 다음
3단계: 시스템 레벨 (MySQL 설정, 하드웨어)
4단계: OS 레벨 (커널 튜닝)
5단계: 아키텍처 레벨 (Redis, 캐싱)

각 단계마다 측정 → 개선 → 검증
→ 필요한 만큼만 최적화 (YAGNI 원칙)
```

### 4. **실제 개선 예상치**

| 최적화 항목 | 난이도 | 비용 | 예상 개선 | 누적 성능 |
|-------------|--------|------|-----------|-----------|
| JPQL Atomic | ⭐ | 0원 | 10배 | 2,000 req/s ✅ |
| 복합 인덱스 | ⭐ | 0원 | 2배 | 4,000 req/s |
| N+1 제거 | ⭐⭐ | 0원 | 3배 | 12,000 req/s |
| MySQL 설정 | ⭐⭐ | 0원 | 1.5배 | 18,000 req/s |
| 하드웨어 UP | ⭐ | $50/월 | 2배 | 36,000 req/s |
| 커널 튜닝 | ⭐⭐⭐ | 0원 | 1.3배 | 47,000 req/s |
| **Redis** | ⭐⭐⭐⭐ | $100/월 | 1.1배 | 50,000 req/s |

**결론: Redis는 마지막 5% 개선을 위한 것!**

---

## 💰 비용 대비 효과 분석

### JPQL Atomic Update (채택)
```
개발 비용: 1시간
인프라 비용: 0원/월
유지보수 비용: 낮음
성능: 2,000 req/s
데드락: 해결 ✅

ROI: ⭐⭐⭐⭐⭐
```

### Redis
```
개발 비용: 1주일 (40시간)
인프라 비용: $50~100/월
유지보수 비용: 높음
성능: 50,000 req/s
데드락: 해결 ✅

ROI (MVP 단계): ⭐ (오버스펙)
```

### 메시지큐
```
개발 비용: 2주일 (80시간)
인프라 비용: $100~200/월
유지보수 비용: 매우 높음
성능: 비동기 (실시간성 낮음)
데드락: 해결 ✅

ROI (카운터 용도): ❌ (부적합)
```

---

## 🎓 결론

### ✅ 선택: JPQL Atomic Update

**이유:**
1. **성능 충분** - 현재 트래픽 대비 20배 여유
2. **구현 간단** - 1시간 만에 적용 가능
3. **비용 0원** - 추가 인프라 불필요
4. **데드락 해결** - 핵심 문제 해결됨
5. **확장 가능** - 필요 시 Redis로 전환 쉬움

### ⚠️ Redis는 "나중에"

**도입 시점:**
- 트래픽이 초당 1,000건 넘을 때
- DB CPU 사용률 70% 넘을 때
- P95 응답속도 100ms 넘을 때

**현재는 과함:**
- 500배 오버스펙
- 불필요한 복잡도
- MVP 단계에 부적합

### ❌ 메시지큐는 "카운터 용도로는 부적합"

**이유:**
- 실시간성 필수 (메시지큐는 비동기)
- 순서 보장 복잡
- 카운터보다 알림/이벤트에 적합

---

## 💬 설명 예시

> "현재 MVP 단계에서는 **JPQL Atomic Update 방식**이 최적입니다.
>
> 이 방식은 **데드락 문제를 완전히 해결**하면서도, 초당 2,000건까지 처리 가능해서 **현재 예상 트래픽(초당 100건)의 20배 여유**가 있습니다.
>
> Redis나 메시지큐도 고려했지만, MVP 단계에서는 **과도한 인프라**입니다:
> - Redis: 50,000 req/s 성능 (현재 필요의 500배)
> - 메시지큐: 카운터는 실시간성이 중요해서 비동기 처리는 부적합
>
> 더 중요한 건, **JPQL → Redis 전환이 매우 쉽다**는 점입니다. 서비스 인터페이스는 동일하고 구현만 바꾸면 되기 때문에, 트래픽이 증가하면 **점진적으로 마이그레이션** 가능합니다.
>
> **비용 대비 효과 측면**에서도:
> - JPQL: 개발 1시간, 인프라 비용 0원
> - Redis: 개발 1주일, 인프라 비용 월 $50~100
>
> MVP에서는 **빠른 출시**와 **안정성**이 우선이므로, JPQL로 시작하고 필요 시 확장하는 전략이 합리적입니다."

---

## 📈 모니터링 계획

### 추적할 지표
```yaml
metrics:
  - name: community_post_view_latency_p95
    threshold: 100ms
    alert: true

  - name: community_post_view_tps
    threshold: 1000 req/s
    alert: true

  - name: mysql_cpu_usage
    threshold: 70%
    alert: true

  - name: deadlock_count
    threshold: 0
    alert: true
```

### 의사결정 트리거
```
IF (tps > 1000 OR cpu > 70% OR p95 > 100ms):
    → Redis 도입 검토 시작
ELSE:
    → JPQL 유지
```

---

## 📚 참고 자료

- [ATOMIC_UPDATE_COMPARISON.md](./ATOMIC_UPDATE_COMPARISON.md) - 방식별 상세 비교
- [DEADLOCK_ANALYSIS.md](./DEADLOCK_ANALYSIS.md) - 데드락 분석
- [COMMUNITY_PERFORMANCE_ISSUES.md](./COMMUNITY_PERFORMANCE_ISSUES.md) - 성능 이슈 분석

---

**최종 결정: JPQL Atomic Update 채택 ✅**
**재검토 시점: 트래픽 초당 1,000건 도달 시**
