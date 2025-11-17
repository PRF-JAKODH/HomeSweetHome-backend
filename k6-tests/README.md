# 📊 k6 부하 테스트 가이드 (로컬 환경)

HomeSweetHome 백엔드 커뮤니티 API의 **로컬 환경** 성능 테스트입니다.

## 🚀 빠른 시작 (2단계)

### 1️⃣ 서버 실행 확인
```bash
# Spring Boot 앱 실행
./gradlew bootRun

# 헬스체크
curl http://localhost:8080/actuator/health
```

### 2️⃣ 테스트 실행
```bash
# ⚡ 빠른 검증 (1분, 추천!)
k6 run k6-tests/community-quick-test.js

# 🎯 실제 사용자 시나리오
k6 run k6-tests/community-realistic-scenario.js

# 💪 부하 테스트 (로컬 환경 최적화)
k6 run k6-tests/community-local-load.js
```

---

## 📁 파일 구조

```
k6-tests/
├── 🆕 community-quick-test.js           # 빠른 기능 검증 (1분)
├── 🆕 community-realistic-scenario.js   # 실제 사용자 시나리오
├── 🆕 community-local-load.js           # 로컬 PC 맞춤 부하 테스트
├── community-load-test.js              # [기존] 기본 부하 테스트
├── community-load-test-simple.js       # [기존] 간단한 부하 테스트
├── stress-test.js                      # [기존] 극한 스트레스 테스트
├── spike-test.js                       # [기존] 스파이크 테스트
├── setup-test-data.js                  # 데이터 확인 스크립트
├── create-test-data.sql                # 테스트 데이터 생성 SQL
└── README.md                           # 이 파일
```

---

## 🎯 로컬 환경용 테스트 시나리오

### 🆕 1. 빠른 기능 검증 (추천!)
**파일:** `community-quick-test.js`
**소요 시간:** 1분
**목적:** 코드 수정 후 빠른 동작 확인

```bash
k6 run k6-tests/community-quick-test.js
```

**특징:**
- ⚡ 5명의 동시 사용자 (로컬 환경 최적화)
- 커뮤니티 핵심 기능 검증
- 90% 이상 성공률 확인

**언제 사용?**
- 코드 수정 직후
- Git commit 전 sanity check
- CI/CD 파이프라인 통합

---

### 🆕 2. 실제 사용자 시나리오
**파일:** `community-realistic-scenario.js`
**소요 시간:** 약 2분
**목적:** 실제 사용자 행동 시뮬레이션

```bash
# 조회만 테스트 (인증 불필요)
k6 run k6-tests/community-realistic-scenario.js

# 전체 기능 테스트 (좋아요, 댓글 작성 포함)
export TEST_TOKEN="Bearer your-jwt-token"
k6 run k6-tests/community-realistic-scenario.js
```

**사용자 여정:**
1. 📋 메인 페이지 진입 → 게시글 목록
2. 👁️ 관심 게시글 클릭 → 상세 조회
3. 📈 조회수 증가
4. 💬 댓글 읽기
5. ❤️ 좋아요 (30% 확률, 인증 필요)
6. ✍️ 댓글 작성 (20% 확률, 인증 필요)

**부하 프로필:**
- 20초: 5명까지 증가
- 40초: 10명 유지
- 20초: 종료

---

### 🆕 3. 로컬 부하 테스트
**파일:** `community-local-load.js`
**소요 시간:** 약 3분
**목적:** 로컬 PC에서 병목 지점 파악

```bash
k6 run k6-tests/community-local-load.js
```

**부하 프로필 (로컬 PC 최적화):**
- 15초: 워밍업 (0 → 10명)
- 30초: 10 → 30명
- 30초: 30 → 50명
- 1분: 피크 유지 (50 → 100명)
- 20초: 쿨다운 (100 → 0명)

**검증 포인트:**
- 🔥 조회수 증가 API 동시성 제어
- 📊 페이지네이션 성능
- 🐌 느린 쿼리 탐지
- 💾 로컬 DB 부하 확인

**성능 기대치:**
- p(95) < 3초 (로컬 DB 고려)
- 에러율 < 15%

---

### 📋 기존 테스트 (참고용)

#### community-load-test.js
- 프로덕션 수준 부하 (10→100명)
- Prometheus 통합 가능

#### stress-test.js / spike-test.js
- 극한 환경 테스트
- ⚠️ 로컬 환경에는 과도할 수 있음

---

## 📊 로컬 환경 기대 결과

### ✅ 정상 케이스 (community-quick-test.js)
```
✓ 목록 조회........: 100.00%
✓ 상세 조회........: 100.00%
✓ 조회수 증가......: 100.00%
✓ 댓글 조회........: 100.00%

http_req_duration..: avg=150ms p(95)=300ms
http_req_failed....: 0.00%
```

### ⚠️ 데이터 없음 (초기 상태)
```
❌ 목록 조회 실패
⚠️  목록 조회 실패 (status: 200)
   → content: [] (빈 배열)

해결: 테스트 데이터 생성 필요
```

### 🐌 성능 저하 (로컬 DB 부하)
```
http_req_duration..: avg=2500ms p(95)=5000ms
errors.............: 15.00%

원인:
- 로컬 MySQL 리소스 부족
- N+1 쿼리 문제
- 인덱스 미설정

해결:
- 쿼리 최적화
- Fetch Join 적용
- 인덱스 추가
```

---

## 🔧 환경 설정 (선택사항)

### 환경변수 커스터마이징
```bash
# 서버 URL 변경
export BASE_URL=http://localhost:9090

# 인증 토큰 설정
export TEST_TOKEN="Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6..."

# 테스트 실행
k6 run k6-tests/community-realistic-scenario.js
```

### 토큰 발급 방법
1. 브라우저에서 http://localhost:8080 접속
2. OAuth2 로그인 (Google/Kakao)
3. 개발자 도구 열기 (F12)
4. Application > Local Storage > 토큰 복사
5. `export TEST_TOKEN="Bearer <복사한토큰>"`

---

## 🔍 모니터링 통합 (선택사항)

### Prometheus + Grafana (고급)
```bash
# 모니터링 스택 실행
docker-compose -f docker-compose.monitoring.yml up -d

# Prometheus 통합 테스트
k6 run --out experimental-prometheus-rw k6-tests/community-local-load.js

# Grafana 확인
open http://localhost:3001
```

**참고:** 로컬 개발 시 모니터링은 선택사항입니다.

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

### ❌ 게시글이 없음 (빈 배열)
```bash
# 원인: DB에 테스트 데이터가 없음

# 해결 1: SQL로 데이터 생성
docker exec -i homesweet-db mysql -u user -ppassword homesweet < k6-tests/create-test-data.sql

# 해결 2: 브라우저에서 수동으로 게시글 작성
open http://localhost:8080
```

### ❌ 서버가 응답하지 않음
```bash
# 1. 서버 실행 확인
curl http://localhost:8080/actuator/health

# 2. 포트 사용 확인
lsof -i :8080

# 3. 서버 재시작
./gradlew bootRun
```

### ❌ 인증 필요한 API 실패 (401 Unauthorized)
```bash
# 원인: TEST_TOKEN 미설정 또는 만료

# 해결: 새 토큰 발급
# 1. 브라우저에서 로그인
# 2. F12 > Application > Local Storage > 토큰 복사
# 3. 환경변수 설정
export TEST_TOKEN="Bearer your-new-token"
k6 run k6-tests/community-realistic-scenario.js
```

### 🐌 테스트가 너무 느림
```bash
# 로컬 PC 사양에 맞게 부하 조정

# 낮은 부하로 시작 (추천)
k6 run k6-tests/community-quick-test.js

# 높은 부하는 피하기
# ❌ k6 run k6-tests/stress-test.js  (로컬에 과부하!)
```

### ⚠️ Docker MySQL 연결 실패
```bash
# DB 컨테이너 상태 확인
docker ps | grep mysql

# DB 재시작
docker-compose up -d
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
