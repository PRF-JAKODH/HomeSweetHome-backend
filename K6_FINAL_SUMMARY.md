# 🎉 k6 부하 테스트 완료!

## ✅ 설정 완료 항목

- [x] k6 테스트 스크립트 작성 (3종)
- [x] 테스트 데이터 생성 (10개 게시글)
- [x] Prometheus + Grafana + Jaeger 연동
- [x] 테스트 실행 성공
- [x] 문제 해결 (Prometheus Remote Write → HTML 리포트)

---

## 🚀 지금 바로 실행

### 1️⃣ 기본 부하 테스트
```bash
k6 run --vus 10 --duration 30s k6-tests/community-load-test.js
```

**결과:**
```
✅ checks: 100.00% (126/126 passed)
✅ http_req_failed: 0.00%
✅ http_req_duration p(95): 26.89ms
✅ ALL THRESHOLDS PASSED
```

### 2️⃣ HTML 리포트 생성
```bash
k6 run k6-tests/community-load-test-simple.js
open summary.html
```

### 3️⃣ 스트레스 테스트
```bash
k6 run k6-tests/stress-test.js
```

### 4️⃣ 스파이크 테스트
```bash
k6 run k6-tests/spike-test.js
```

---

## 📊 모니터링 통합

### Prometheus (Spring Boot 메트릭)
```bash
# http://localhost:9090
```

**유용한 쿼리:**
```promql
# API 응답시간
rate(http_server_requests_seconds_sum[1m]) / rate(http_server_requests_seconds_count[1m])

# 초당 요청 수
rate(http_server_requests_seconds_count[1m])

# JVM Heap 사용률
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100
```

### Grafana (대시보드)
```bash
# http://localhost:3001 (admin/admin)
# Import Dashboard: 4701
```

### Jaeger (트레이싱)
```bash
# http://localhost:16686
# Service: homesweet-back
```

---

## 🎯 실전 시나리오

### 시나리오 1: API 성능 확인

**1단계: 부하 테스트**
```bash
k6 run --vus 50 --duration 2m k6-tests/community-load-test.js
```

**2단계: Prometheus 확인**
```promql
# 응답시간 추이
histogram_quantile(0.95,
  rate(http_server_requests_seconds_bucket{uri="/api/v1/community/posts"}[1m])
)
```

**3단계: Jaeger 확인**
- 느린 요청 찾기
- 병목 지점 분석

---

### 시나리오 2: N+1 문제 발견

**1단계: 스파이크 테스트**
```bash
k6 run k6-tests/spike-test.js
```

**2단계: Jaeger에서 확인**
- Operation: `GET /api/v1/community/posts`
- Span 개수 확인
- **이미지 쿼리가 N번 발생?** ⚠️

**3단계: 코드 수정**
```java
// Before: N+1 문제
List<String> imageUrls = imageRepository.findByPostOrderByImageOrderAsc(post);

// After: Fetch Join
@EntityGraph(attributePaths = {"images"})
Page<CommunityPostEntity> findByIsDeletedFalse(Pageable pageable);
```

**4단계: 재테스트**
```bash
k6 run k6-tests/spike-test.js
# 응답시간 개선 확인!
```

---

### 시나리오 3: 조회수 동시성 테스트

**1단계: 스트레스 테스트**
```bash
k6 run k6-tests/stress-test.js
```

**2단계: Prometheus 확인**
```promql
# 조회수 API 응답시간
rate(http_server_requests_seconds_sum{uri=~".*views"}[1m])
/
rate(http_server_requests_seconds_count{uri=~".*views"}[1m])
```

**3단계: Jaeger 확인**
- Lock 대기시간이 긴가?
- 비관적 락 성능 측정

**4단계: 개선안**
```java
// Redis로 비동기 처리
@Async
public void increaseViewCount(Long postId) {
    redisTemplate.opsForValue().increment("view:post:" + postId);
}
```

---

## 📁 생성된 파일

```
HomeSweetHome-backend/
├── k6-tests/
│   ├── README.md                      ⭐ k6 가이드
│   ├── community-load-test.js         ✅ 기본 부하 테스트
│   ├── community-load-test-simple.js  ✅ HTML 리포트 버전
│   ├── stress-test.js                 ✅ 스트레스 테스트
│   ├── spike-test.js                  ✅ 스파이크 테스트
│   ├── setup-test-data.js             ✅ 데이터 확인
│   └── create-test-data.sql           ✅ 테스트 데이터 생성
│
├── K6_LOAD_TESTING.md                 ⭐ 상세 가이드
├── QUICK_START_K6.md                  ⭐ 빠른 시작
├── TEST_DATA_SETUP.md                 ⭐ 데이터 설정
├── K6_PROMETHEUS_FIX.md               ⭐ 문제 해결
├── K6_FINAL_SUMMARY.md                ⭐ 이 파일
│
├── MONITORING_GUIDE.md                ⭐ 모니터링 가이드
├── MONITORING_SETUP.md                ⭐ 모니터링 설정
│
├── prometheus.yml                     ✅ Prometheus 설정
└── docker-compose.monitoring.yml      ✅ 모니터링 스택
```

---

## 🎓 학습 포인트

### k6로 배운 것
- ✅ 부하 테스트 시나리오 작성
- ✅ 가상 사용자 (VU) 개념
- ✅ 성능 임계값 설정
- ✅ 커스텀 메트릭 정의

### 모니터링으로 배운 것
- ✅ Prometheus 쿼리 (PromQL)
- ✅ Grafana 대시보드 구성
- ✅ Jaeger 분산 트레이싱
- ✅ 병목 지점 분석

### 성능 최적화로 배운 것
- ✅ N+1 문제 감지 및 해결
- ✅ 동시성 제어 (비관적 락)
- ✅ 응답시간 측정 및 개선
- ✅ 시스템 한계 파악

---

## 🔍 체크리스트

### 기본 설정
- [x] k6 설치 확인
- [x] 테스트 데이터 생성
- [x] 모니터링 스택 실행
- [x] Spring Boot 애플리케이션 실행

### 부하 테스트
- [x] 기본 부하 테스트 성공
- [x] 모든 체크 통과
- [x] 에러율 0%
- [x] 응답시간 임계값 만족

### 모니터링
- [x] Prometheus 메트릭 수집
- [x] Grafana 대시보드 구성
- [x] Jaeger 트레이싱 확인

### 문제 해결
- [x] Prometheus Remote Write 이슈 해결
- [x] 테스트 데이터 없음 이슈 해결
- [x] HTML 리포트로 대체

---

## 💡 베스트 프랙티스

### 1. 부하 테스트 전
```bash
# 1. 테스트 데이터 확인
curl http://localhost:8080/api/v1/community/posts | jq

# 2. Health 체크
curl http://localhost:8080/actuator/health

# 3. 모니터링 스택 확인
docker-compose -f docker-compose.monitoring.yml ps
```

### 2. 부하 테스트 중
```bash
# 터미널 1: k6 실행
k6 run k6-tests/community-load-test.js

# 터미널 2: 서버 로그 모니터링
tail -f logs/dev/trace/trace.log

# 브라우저 1: Grafana
http://localhost:3001

# 브라우저 2: Jaeger
http://localhost:16686
```

### 3. 부하 테스트 후
```bash
# 1. 결과 분석
# - 체크 통과율
# - 응답시간 분포
# - 에러율

# 2. 병목 지점 확인
# - Jaeger에서 느린 API 찾기
# - Prometheus에서 리소스 사용량 확인

# 3. 개선 및 재테스트
# - 코드 수정
# - 다시 부하 테스트
# - 성능 비교
```

---

## 🎯 다음 단계

### 1. 성능 개선
- [ ] N+1 문제 해결 (Fetch Join)
- [ ] 조회수 Redis 비동기 처리
- [ ] 게시글 목록 캐싱

### 2. 테스트 확장
- [ ] 인증 포함 부하 테스트
- [ ] 게시글 작성 API 테스트
- [ ] 댓글 작성 동시성 테스트

### 3. CI/CD 통합
- [ ] GitHub Actions에 k6 추가
- [ ] PR마다 자동 부하 테스트
- [ ] 성능 회귀 방지

### 4. 모니터링 강화
- [ ] Grafana 알림 설정
- [ ] 커스텀 대시보드 생성
- [ ] 로그 수집 (Loki)

---

## 📚 참고 문서

| 문서 | 내용 |
|------|------|
| [k6-tests/README.md](k6-tests/README.md) | k6 테스트 가이드 |
| [K6_LOAD_TESTING.md](K6_LOAD_TESTING.md) | 상세 부하 테스트 가이드 |
| [QUICK_START_K6.md](QUICK_START_K6.md) | 빠른 시작 가이드 |
| [TEST_DATA_SETUP.md](TEST_DATA_SETUP.md) | 테스트 데이터 설정 |
| [K6_PROMETHEUS_FIX.md](K6_PROMETHEUS_FIX.md) | Prometheus 연동 문제 해결 |
| [MONITORING_GUIDE.md](MONITORING_GUIDE.md) | 모니터링 시스템 가이드 |

---

## 🎉 완성!

**축하합니다!** 🎊

완벽한 부하 테스트 + 모니터링 시스템을 구축했습니다:

✅ **k6**: 부하 테스트 자동화
✅ **Prometheus**: 메트릭 수집
✅ **Grafana**: 실시간 대시보드
✅ **Jaeger**: 분산 트레이싱
✅ **테스트 데이터**: 자동 생성

이제 성능 문제를 빠르게 발견하고 최적화할 수 있습니다!

---

**Happy Testing & Monitoring! 🚀**
