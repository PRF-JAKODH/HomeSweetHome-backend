# 🚀 k6 부하 테스트 + 모니터링 통합 가이드

k6로 부하 테스트를 실행하면서 Prometheus + Grafana + Jaeger로 실시간 모니터링하는 완벽한 가이드입니다.

## 📦 준비사항

### 1. k6 설치 확인
```bash
k6 version
# k6 v0.x.x 출력되면 OK
```

설치 안되어 있다면:
```bash
# macOS
brew install k6

# Ubuntu/Debian
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6

# Windows (Chocolatey)
choco install k6
```

### 2. 모니터링 스택 실행
```bash
# Prometheus + Grafana + Jaeger 실행
docker-compose -f docker-compose.monitoring.yml up -d

# 상태 확인
docker-compose -f docker-compose.monitoring.yml ps
```

### 3. Spring Boot 애플리케이션 실행
```bash
./gradlew bootRun
```

---

## 🎯 부하 테스트 시나리오

### 1. 기본 부하 테스트 (Load Test)

**목적:** 정상 트래픽에서 성능 확인

```bash
# 기본 실행
k6 run k6-tests/community-load-test.js

# Prometheus 통합 실행
k6 run --out experimental-prometheus-rw k6-tests/community-load-test.js
```

**시나리오:**
- 30초: 0 → 10명
- 1분: 10 → 50명
- 2분: 50 → 100명 유지
- 30초: 100 → 0명

**테스트 내용:**
1. 게시글 목록 조회
2. 특정 게시글 조회
3. 조회수 증가
4. 댓글 조회

### 2. 스트레스 테스트 (Stress Test)

**목적:** 시스템 한계점 찾기

```bash
k6 run k6-tests/stress-test.js
```

**시나리오:**
- 1분: 0 → 50명
- 2분: 50 → 100명
- 3분: 100 → 200명
- 2분: 200 → 500명 🔥 (한계 테스트)
- 1분: 500 → 0명

### 3. 스파이크 테스트 (Spike Test)

**목적:** 급격한 트래픽 증가 대응

```bash
k6 run k6-tests/spike-test.js
```

**시나리오:**
- 10초: 10명 (정상)
- 10초: 10 → 500명 ⚡ (급증!)
- 30초: 500명 유지
- 10초: 500 → 10명
- 20초: 회복

---

## 📊 k6 + Prometheus 통합

### 방법 1: Prometheus Remote Write (추천!)

k6에서 직접 Prometheus로 메트릭 전송:

```bash
k6 run \
  --out experimental-prometheus-rw \
  k6-tests/community-load-test.js
```

기본 엔드포인트: `http://localhost:9090/api/v1/write`

### 방법 2: 커스텀 Prometheus 엔드포인트

```bash
K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write \
K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM=true \
k6 run --out experimental-prometheus-rw k6-tests/community-load-test.js
```

### k6 메트릭 확인

Prometheus에서 조회:
```promql
# k6 HTTP 요청 수
k6_http_reqs_total

# k6 요청 실패율
k6_http_req_failed

# k6 응답시간 (p95)
k6_http_req_duration{quantile="0.95"}

# k6 활성 VU (Virtual Users)
k6_vus
```

---

## 🎨 Grafana 대시보드 설정

### 1. k6 공식 대시보드 Import

1. Grafana 접속: http://localhost:3001
2. **Dashboards** > **New** > **Import**
3. 대시보드 ID 입력: **2587** (k6 Prometheus)
4. **Load** 클릭
5. Prometheus 데이터소스 선택
6. **Import** 클릭

### 2. 통합 대시보드 만들기

**k6 + Spring Boot 통합 모니터링**

새 대시보드 생성 후 다음 패널 추가:

#### 패널 1: k6 Virtual Users vs API 응답시간
```promql
# k6 VU
k6_vus

# Spring Boot 응답시간
rate(http_server_requests_seconds_sum[1m]) / rate(http_server_requests_seconds_count[1m])
```

#### 패널 2: k6 요청 vs 실제 서버 요청
```promql
# k6에서 보낸 요청
rate(k6_http_reqs_total[1m])

# 서버에서 받은 요청
rate(http_server_requests_seconds_count[1m])
```

#### 패널 3: 에러율 비교
```promql
# k6 에러율
rate(k6_http_req_failed{expected_response="true"}[1m])

# 서버 5xx 에러
rate(http_server_requests_seconds_count{status=~"5.."}[1m])
```

#### 패널 4: JVM 메모리 (부하 테스트 중)
```promql
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100
```

#### 패널 5: DB 커넥션 풀 (부하 테스트 중)
```promql
hikaricp_connections_active
```

---

## 🔍 Jaeger로 트레이싱 확인

### 부하 테스트 중 트레이스 분석

1. http://localhost:16686 접속
2. Service: `homesweet-back` 선택
3. **Lookback**: Last 5 minutes
4. **Find Traces** 클릭

### 확인 포인트

#### N+1 문제 감지
- Operation: `GET /api/v1/community/posts`
- Span 개수 확인
- 데이터베이스 쿼리 횟수

#### 느린 쿼리 찾기
- Duration으로 정렬
- 가장 느린 Trace 확인
- 어느 부분에서 시간이 오래 걸리는지 분석

#### 동시성 문제 확인
- Operation: `POST /api/v1/community/posts/{postId}/views`
- Lock 대기 시간 확인
- 비관적 락 성능 측정

---

## 📈 실전 시나리오

### 시나리오 1: N+1 문제 확인

**1단계: 부하 테스트 실행**
```bash
k6 run --out experimental-prometheus-rw k6-tests/spike-test.js
```

**2단계: Jaeger에서 확인**
- `GET /api/v1/community/posts` 트레이스 조회
- 이미지 조회 쿼리가 게시글 수만큼 발생하는지 확인

**3단계: Grafana에서 메트릭 확인**
```promql
# 평균 응답시간이 느린지 확인
histogram_quantile(0.95,
  rate(http_server_requests_seconds_bucket{uri="/api/v1/community/posts"}[5m])
)
```

### 시나리오 2: 조회수 증가 동시성 테스트

**1단계: 스트레스 테스트 실행**
```bash
k6 run --out experimental-prometheus-rw k6-tests/stress-test.js
```

**2단계: Prometheus에서 확인**
```promql
# 조회수 증가 API 응답시간
rate(http_server_requests_seconds_sum{uri="/api/v1/community/posts/{postId}/views"}[1m])
/
rate(http_server_requests_seconds_count{uri="/api/v1/community/posts/{postId}/views"}[1m])
```

**3단계: Jaeger에서 Lock 대기시간 확인**
- Lock이 오래 걸리면 Redis로 개선 필요

### 시나리오 3: 메모리 누수 확인

**1단계: 장시간 부하 테스트**
```bash
k6 run --duration 10m --vus 100 k6-tests/community-load-test.js
```

**2단계: Grafana에서 JVM 메모리 추이 확인**
```promql
# Heap 메모리 사용량
jvm_memory_used_bytes{area="heap"}

# GC 횟수 증가 추이
rate(jvm_gc_pause_seconds_count[1m])
```

---

## 🎯 성능 임계값 설정

### k6 테스트 스크립트에 임계값 설정

```javascript
export const options = {
  thresholds: {
    // 95%의 요청이 500ms 이내
    'http_req_duration': ['p(95)<500'],

    // 99%의 요청이 1초 이내
    'http_req_duration': ['p(99)<1000'],

    // 에러율 1% 미만
    'http_req_failed': ['rate<0.01'],

    // 특정 API 임계값
    'http_req_duration{name:GetPost}': ['p(95)<200'],
    'http_req_duration{name:IncreaseViewCount}': ['p(95)<100'],
  },
};
```

---

## 📊 커스텀 메트릭 추가

### k6 스크립트에 비즈니스 메트릭 추가

```javascript
import { Counter, Trend, Rate } from 'k6/metrics';

// 커스텀 메트릭 정의
const postCreatedCount = new Counter('post_created');
const likeClickRate = new Rate('like_clicked');
const commentLoadTime = new Trend('comment_load_time');

export default function () {
  // 게시글 조회
  const response = http.get(`${BASE_URL}/api/v1/community/posts/1`);

  // 댓글 조회 시간 측정
  const start = new Date();
  const comments = http.get(`${BASE_URL}/api/v1/community/posts/1/comments`);
  commentLoadTime.add(new Date() - start);

  // 좋아요 클릭
  if (Math.random() > 0.7) {
    http.post(`${BASE_URL}/api/v1/community/posts/1/likes`);
    likeClickRate.add(1);
  } else {
    likeClickRate.add(0);
  }
}
```

Prometheus에서 조회:
```promql
k6_post_created_total
k6_like_clicked
k6_comment_load_time
```

---

## 🚨 알림 설정

### Grafana Alert 설정

1. **패널 편집** > **Alert** 탭
2. 조건 설정:
   ```
   WHEN avg() OF query(A, 5m) IS ABOVE 1000
   ```
3. **Notification channel** 설정 (Slack, Discord 등)

### 예시 알림 규칙

**응답시간 초과**
```
Alert: API 응답시간 1초 초과
Query: rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m]) > 1
```

**에러율 급증**
```
Alert: 에러율 5% 초과
Query: rate(http_server_requests_seconds_count{status=~"5.."}[1m]) / rate(http_server_requests_seconds_count[1m]) > 0.05
```

---

## 🔄 CI/CD 통합

### GitHub Actions에서 부하 테스트

```yaml
# .github/workflows/load-test.yml
name: Load Test

on:
  pull_request:
    branches: [develop, main]

jobs:
  load-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Start monitoring stack
        run: docker-compose -f docker-compose.monitoring.yml up -d

      - name: Start application
        run: ./gradlew bootRun &

      - name: Wait for application
        run: sleep 30

      - name: Run k6 load test
        run: k6 run --out experimental-prometheus-rw k6-tests/community-load-test.js

      - name: Check thresholds
        run: |
          if [ $? -ne 0 ]; then
            echo "Load test failed!"
            exit 1
          fi
```

---

## 📚 유용한 k6 옵션

### 다양한 실행 옵션

```bash
# VU(가상 사용자) 수 지정
k6 run --vus 100 --duration 5m script.js

# 최대 VU 수 제한
k6 run --vus 10 --max-vus 100 script.js

# JSON 결과 출력
k6 run --out json=results.json script.js

# 여러 output 동시 사용
k6 run \
  --out json=results.json \
  --out experimental-prometheus-rw \
  script.js

# HTTP 디버그 모드
k6 run --http-debug script.js

# 태그로 특정 시나리오만 실행
k6 run --tag testid=run01 script.js
```

---

## 🎓 다음 단계

1. **성능 최적화**
   - 부하 테스트 결과를 바탕으로 N+1 문제 해결
   - 조회수 증가를 Redis로 비동기 처리
   - DB 인덱스 최적화

2. **캐싱 전략**
   - 인기 게시글 Redis 캐싱
   - 응답시간 개선 확인

3. **스케일링 테스트**
   - 서버 2대로 로드밸런싱
   - 부하 분산 확인

4. **장애 시나리오 테스트**
   - DB 연결 끊김
   - 메모리 부족 상황

---

## 📖 참고 자료

- [k6 공식 문서](https://k6.io/docs/)
- [k6 Prometheus Output](https://k6.io/docs/results-output/real-time/prometheus-remote-write/)
- [Grafana k6 대시보드](https://grafana.com/grafana/dashboards/2587)
- [성능 테스트 베스트 프랙티스](https://k6.io/docs/testing-guides/test-types/)

---

**Happy Load Testing! 🚀**
