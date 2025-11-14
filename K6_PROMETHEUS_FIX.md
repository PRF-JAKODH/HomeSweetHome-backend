# 🔧 k6 + Prometheus 연동 문제 해결

## ❌ 문제: Prometheus Remote Write 404 에러

```
ERRO Failed to send the time series data to the endpoint
error="got status code: 404 instead expected a 2xx successful status code"
```

### 원인
Prometheus 3.x 버전에서는 Remote Write 수신 기능이 기본적으로 **비활성화**되어 있습니다.

---

## ✅ 해결 방법 (3가지)

### 방법 1: HTML 리포트 사용 (가장 간단! 추천)

k6에서 자체적으로 예쁜 HTML 리포트를 생성합니다.

```bash
# HTML 리포트 생성
k6 run k6-tests/community-load-test-simple.js

# 브라우저에서 열기
open summary.html
```

**장점:**
- ✅ 설정 불필요
- ✅ 예쁜 차트와 그래프
- ✅ 공유 가능 (HTML 파일)
- ✅ 오프라인에서도 볼 수 있음

**리포트 내용:**
- 📊 전체 테스트 요약
- 📈 응답시간 분포 그래프
- ✅ 통과/실패한 체크
- 🎯 임계값 달성 여부

---

### 방법 2: Spring Boot 메트릭만 사용

k6는 부하만 생성하고, Prometheus는 Spring Boot 메트릭만 수집합니다.

**1단계: k6로 부하 생성**
```bash
# 터미널 1: k6 실행
k6 run k6-tests/community-load-test.js
```

**2단계: Prometheus에서 Spring Boot 메트릭 확인**
```bash
# 브라우저에서 http://localhost:9090 접속
```

**Prometheus 쿼리:**
```promql
# API 응답시간
rate(http_server_requests_seconds_sum[1m]) / rate(http_server_requests_seconds_count[1m])

# 초당 요청 수
rate(http_server_requests_seconds_count[1m])

# 에러율
rate(http_server_requests_seconds_count{status=~"5.."}[1m])

# JVM Heap 사용률
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100
```

**3단계: Grafana에서 시각화**
```bash
# http://localhost:3001 접속
# Dashboard Import: 4701 (JVM Micrometer)
```

**장점:**
- ✅ k6와 Prometheus 독립적 사용
- ✅ Spring Boot 메트릭은 정상 수집
- ✅ 설정 간단

---

### 방법 3: Prometheus 2.x로 다운그레이드 (권장하지 않음)

```yaml
# docker-compose.monitoring.yml
services:
  prometheus:
    image: prom/prometheus:v2.53.3  # 2.x 버전 사용
    # ... 나머지 설정
```

**단점:**
- ❌ 구버전 사용
- ❌ 최신 기능 사용 불가

---

## 🎯 권장 워크플로우

### 시나리오 1: 빠른 부하 테스트

```bash
# 1. HTML 리포트 생성
k6 run k6-tests/community-load-test-simple.js

# 2. 브라우저에서 결과 확인
open summary.html
```

**확인 사항:**
- ✅ 모든 체크 통과?
- ✅ 응답시간 임계값 만족?
- ✅ 에러율 낮은가?

---

### 시나리오 2: 상세 모니터링

**터미널 1:**
```bash
k6 run --vus 100 --duration 5m k6-tests/community-load-test.js
```

**터미널 2:**
```bash
# Prometheus 쿼리 실행
curl 'http://localhost:9090/api/v1/query?query=rate(http_server_requests_seconds_count[1m])'
```

**브라우저:**
- http://localhost:3001 - Grafana 대시보드
- http://localhost:16686 - Jaeger 트레이싱

---

### 시나리오 3: N+1 문제 분석

**1단계: 부하 생성**
```bash
k6 run k6-tests/spike-test.js
```

**2단계: Jaeger에서 트레이스 확인**
```bash
# http://localhost:16686
# Service: homesweet-back
# Operation: GET /api/v1/community/posts
```

**3단계: 쿼리 횟수 확인**
- 게시글 조회: 1번
- 이미지 조회: N번 ⚠️ (문제!)

**4단계: Prometheus에서 응답시간 확인**
```promql
histogram_quantile(0.95,
  rate(http_server_requests_seconds_bucket{uri="/api/v1/community/posts"}[1m])
)
```

---

## 📊 k6 결과 분석

### 터미널 출력 읽기

```
✓ checks................: 100.00%  132 passed
✓ http_req_duration...: avg=15.86ms p(95)=23.87ms
✓ http_req_failed.....: 0.00%
✓ iterations..........: 22
```

**해석:**
- ✅ **checks 100%**: 모든 검증 통과
- ✅ **p(95) 23.87ms**: 95%의 요청이 23.87ms 이내
- ✅ **failed 0%**: 에러 없음
- ✅ **22 iterations**: 22번의 완전한 시나리오 실행

---

## 🎨 HTML 리포트 예시

`summary.html`에 포함되는 내용:

**1. 전체 요약**
- 총 요청 수
- 성공/실패 비율
- 평균 응답시간

**2. 응답시간 분포**
```
       min    avg    med    max    p(90)  p(95)
http   3ms   15ms   16ms   38ms   23ms   23ms
```

**3. 체크 결과**
```
✓ list status is 200       132/132  (100%)
✓ view status is 200       132/132  (100%)
✓ comments status is 200   132/132  (100%)
```

**4. 임계값 달성**
```
✓ http_req_duration p(95)<500ms    ✓ PASSED
✓ http_req_failed rate<0.01        ✓ PASSED
```

---

## 💡 모범 사례

### Before (Prometheus Remote Write 필요)
```bash
❌ k6 run --out experimental-prometheus-rw k6-tests/community-load-test.js
# ERRO Failed to send the time series data
```

### After (독립적 사용)
```bash
# 1. k6로 부하 테스트 + HTML 리포트
✅ k6 run k6-tests/community-load-test-simple.js

# 2. Prometheus로 Spring Boot 모니터링
✅ http://localhost:9090
# Query: rate(http_server_requests_seconds_count[1m])

# 3. Jaeger로 트레이싱
✅ http://localhost:16686
# Service: homesweet-back
```

**결과:**
- ✅ k6: 부하 테스트 결과 (HTML)
- ✅ Prometheus: 서버 메트릭
- ✅ Jaeger: API 트레이싱
- ✅ 모두 독립적으로 작동!

---

## 📚 참고 자료

- [k6 HTML Reporter](https://github.com/benc-uk/k6-reporter)
- [k6 Results Output](https://k6.io/docs/results-output/real-time/)
- [Prometheus Remote Write](https://prometheus.io/docs/prometheus/latest/querying/api/#remote-write-receiver)

---

## 🎯 결론

**Prometheus Remote Write 없이도 완벽한 모니터링 가능!**

1. **k6**: HTML 리포트로 부하 테스트 결과 확인
2. **Prometheus**: Spring Boot 메트릭 수집
3. **Grafana**: 실시간 대시보드
4. **Jaeger**: API 트레이싱

각 도구의 장점을 최대한 활용하면서 독립적으로 사용하는 것이 더 안정적입니다!

---

**Happy Testing! 🚀**
