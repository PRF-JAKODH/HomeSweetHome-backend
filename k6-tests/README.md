# 📊 k6 부하 테스트 가이드

HomeSweetHome 백엔드 API의 성능 테스트 및 모니터링 통합 시스템입니다.

## 🚀 빠른 시작 (3단계)

### 1️⃣ 테스트 데이터 생성
```bash
docker exec -i homesweet-db mysql -u user -ppassword homesweet < k6-tests/create-test-data.sql
```

### 2️⃣ 모니터링 스택 실행
```bash
docker-compose -f docker-compose.monitoring.yml up -d
```

### 3️⃣ 부하 테스트 실행
```bash
k6 run --out experimental-prometheus-rw k6-tests/community-load-test.js
```

---

## 📁 파일 구조

```
k6-tests/
├── community-load-test.js    # 기본 부하 테스트 (10→100명)
├── stress-test.js             # 스트레스 테스트 (최대 500명)
├── spike-test.js              # 스파이크 테스트 (급증 시나리오)
├── setup-test-data.js         # 데이터 확인 스크립트
├── create-test-data.sql       # 테스트 데이터 생성 SQL
└── README.md                  # 이 파일
```

---

## 🎯 테스트 시나리오

### 1. 기본 부하 테스트 (Load Test)
**파일:** `community-load-test.js`
**목적:** 정상 트래픽 성능 측정

```bash
k6 run --out experimental-prometheus-rw k6-tests/community-load-test.js
```

**부하 프로필:**
- 30초: 0 → 10명
- 1분: 10 → 50명
- 2분: 50 → 100명 유지
- 30초: 100 → 0명

**테스트 API:**
- GET /api/v1/community/posts (목록)
- GET /api/v1/community/posts/{id} (상세)
- POST /api/v1/community/posts/{id}/views (조회수)
- GET /api/v1/community/posts/{id}/comments (댓글)

### 2. 스트레스 테스트 (Stress Test)
**파일:** `stress-test.js`
**목적:** 시스템 한계점 찾기

```bash
k6 run --out experimental-prometheus-rw k6-tests/stress-test.js
```

**부하 프로필:**
- 1분: 0 → 50명
- 2분: 50 → 100명
- 3분: 100 → 200명
- 2분: 200 → 500명 🔥
- 1분: 500 → 0명

**포커스:**
- 조회수 증가 API (동시성 제어)
- 비관적 락 성능

### 3. 스파이크 테스트 (Spike Test)
**파일:** `spike-test.js`
**목적:** 급격한 트래픽 증가 대응

```bash
k6 run --out experimental-prometheus-rw k6-tests/spike-test.js
```

**부하 프로필:**
- 10초: 10명 (정상)
- 10초: 10 → 500명 ⚡ (급증!)
- 30초: 500명 유지
- 10초: 500 → 10명
- 20초: 회복

**포커스:**
- 게시글 목록 조회 (N+1 문제)
- 캐시 효율성

---

## 📊 기대 결과

### ✅ 성공 케이스
```
checks_succeeded...: 100.00%
http_req_failed....: 0.00%
errors.............: 0.00%
http_req_duration..: p(95)<500ms
```

### ❌ 실패 케이스 (수정 필요)
```
http_req_failed....: 50.00%  # 게시글이 없음!
errors.............: 100.00% # 데이터 생성 필요
```

---

## 🔍 모니터링 통합

### Prometheus 메트릭
```promql
# k6 가상 사용자
k6_vus

# k6 요청 수
rate(k6_http_reqs_total[1m])

# k6 응답시간 p95
k6_http_req_duration{quantile="0.95"}

# 서버 응답시간
rate(http_server_requests_seconds_sum[1m]) / rate(http_server_requests_seconds_count[1m])
```

### Grafana 대시보드
1. http://localhost:3001 접속
2. Import Dashboard ID: **2587**
3. 실시간 메트릭 확인

### Jaeger 트레이싱
1. http://localhost:16686 접속
2. Service: `homesweet-back`
3. 느린 요청 분석

---

## 🎨 커스텀 메트릭

### 비즈니스 메트릭 추가
```javascript
import { Counter, Trend } from 'k6/metrics';

const postCreated = new Counter('post_created');
const commentLoadTime = new Trend('comment_load_time');

export default function () {
  const start = new Date();
  const comments = http.get(`${BASE_URL}/api/v1/community/posts/1/comments`);
  commentLoadTime.add(new Date() - start);
}
```

---

## 📈 성능 임계값

### 기본 임계값 (k6 옵션)
```javascript
export const options = {
  thresholds: {
    'http_req_duration': ['p(95)<500'],   // 95% 요청이 500ms 이내
    'http_req_duration': ['p(99)<1000'],  // 99% 요청이 1초 이내
    'http_req_failed': ['rate<0.01'],     // 에러율 1% 미만
  },
};
```

### API별 임계값
```javascript
thresholds: {
  'http_req_duration{name:GetPost}': ['p(95)<200'],
  'http_req_duration{name:IncreaseViewCount}': ['p(95)<100'],
}
```

---

## 🛠 문제 해결

### 에러: 게시글이 없음 (50% 실패)
```bash
# 해결: 테스트 데이터 생성
docker exec -i homesweet-db mysql -u user -ppassword homesweet < k6-tests/create-test-data.sql
```

### k6 메트릭이 Prometheus에 안보임
```bash
# 1. Prometheus Remote Write 활성화 확인
docker logs homesweet-prometheus | grep "remote-write"

# 2. k6 실행시 --out 옵션 확인
k6 run --out experimental-prometheus-rw k6-tests/community-load-test.js
```

### 애플리케이션 응답 없음
```bash
# Health 체크
curl http://localhost:8080/actuator/health

# 로그 확인
./gradlew bootRun
```

---

## 📚 참고 문서

- [K6_LOAD_TESTING.md](../K6_LOAD_TESTING.md) - 상세 가이드
- [QUICK_START_K6.md](../QUICK_START_K6.md) - 빠른 시작
- [TEST_DATA_SETUP.md](../TEST_DATA_SETUP.md) - 데이터 설정
- [MONITORING_GUIDE.md](../MONITORING_GUIDE.md) - 모니터링 가이드

---

## 🎓 다음 단계

1. **N+1 문제 해결**
   - Jaeger에서 쿼리 횟수 확인
   - Fetch Join 적용
   - 성능 비교

2. **조회수 최적화**
   - Redis 비동기 처리
   - 부하 테스트로 검증

3. **캐싱 전략**
   - 인기 게시글 캐싱
   - 응답시간 개선 측정

---

**Happy Load Testing! 🚀**
