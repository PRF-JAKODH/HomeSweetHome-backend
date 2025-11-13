# 커뮤니티 k6 부하테스트

커뮤니티 서비스의 모든 기능과 동시성 제어를 검증하는 종합 부하테스트입니다.

## 🎯 테스트 범위

✅ **게시글 CRUD** - 작성, 조회, 수정, 삭제
✅ **댓글 CRUD** - 작성, 조회, 수정, 삭제
✅ **게시글 좋아요** - 토글 & 상태 확인
✅ **댓글 좋아요** - 토글 & 상태 확인
✅ **조회수 증가** - 동시성 제어 검증
✅ **페이지네이션** - 목록 조회
✅ **동시성 제어** - 100명 동시 좋아요 검증

## 🚀 빠른 시작

### 1. 애플리케이션 실행
```bash
./gradlew bootRun
```

### 2. 테스트 실행 (단 하나의 명령어!)
```bash
./src/main/java/com/homesweet/homesweetback/common/k6/community/run-community-test.sh
```

또는 프로젝트 루트에서:
```bash
bash src/main/java/com/homesweet/homesweetback/common/k6/community/run-community-test.sh
```

끝! 이게 전부입니다. 🎉

## 📊 테스트 시나리오

| 시나리오 | 시간 | 설명 |
|---------|------|------|
| 1. CRUD 작업 | 2분 | 게시글/댓글 생성/조회/수정/삭제 |
| 2. 동시 좋아요 | 1분 | **100명이 각각 10번씩 좋아요 (총 1000회)** |
| 3. 동시 조회수 | 30초 | 50명이 동시에 조회 |
| 4. 사용자 여정 | 3분 | 실제 사용자 행동 시뮬레이션 |
| 5. 스트레스 테스트 | 7분 | 최대 300명까지 부하 |

**총 예상 시간: 약 13분**

## ✅ 성공 기준

### 응답 시간
- **p95 < 1000ms** (95%의 요청이 1초 이내)
- **p99 < 2000ms** (99%의 요청이 2초 이내)

### 에러율
- **전체 실패율 < 5%**
- **API 성공률 > 95%**
- **각 기능별 에러 < 10개**

### 성능
- **게시글 작성 p95 < 1500ms**
- **게시글 조회 p95 < 500ms**
- **좋아요 p95 < 800ms**

## 🔍 동시성 제어 검증

테스트 완료 후 데이터베이스에서 확인:

```sql
-- 게시글의 like_count 확인
SELECT post_id, like_count FROM community_posts WHERE post_id = 1;

-- 실제 좋아요 레코드 수 확인
SELECT COUNT(*) FROM community_post_likes WHERE post_id = 1;
```

**✅ 성공:** 두 값이 정확히 일치 (예: 100/100)
**❌ 실패:** like_count < 실제 레코드 수 (데이터 정합성 깨짐)

## ⚙️ 환경 변수 설정 (선택사항)

```bash
# 기본값으로도 충분하지만, 필요시 변경 가능
BASE_URL=http://localhost:8080 \
TEST_POST_ID=5 \
AUTH_TOKEN=your_token \
bash src/main/java/com/homesweet/homesweetback/common/k6/community/run-community-test.sh
```

## 📈 결과 해석

테스트 완료 후 다음과 같은 결과를 확인할 수 있습니다:

```
========================================
  커뮤니티 전체 기능 테스트 결과
========================================

📊 총 요청 수: 15,432

⏱️  응답 시간:
  - 평균: 245.32ms
  - 중간값: 198.45ms
  - 90%ile: 456.78ms
  - 95%ile: 612.34ms ✅
  - 99%ile: 1,234.56ms ✅
  - 최대: 2,345.67ms

✅ 실패율: 0.12% (목표: < 5%)
✅ API 성공률: 99.88% (목표: > 95%)

📝 기능별 에러:
  ✅ 게시글 작성: 0개 (목표: < 5)
  ✅ 게시글 조회: 0개 (목표: < 5)
  ✅ 좋아요: 2개 (목표: < 10)
  ✅ 조회수: 1개 (목표: < 10)

⚡ 성능 메트릭:
  ✅ 게시글 작성 p95: 892.45ms (목표: < 1500ms)
  ✅ 게시글 조회 p95: 234.56ms (목표: < 500ms)
  ✅ 좋아요 p95: 567.89ms (목표: < 800ms)

✅ 체크 통과율: 98.76%

========================================
💾 상세 결과: src/main/java/com/homesweet/homesweetback/common/community-test-results.json
========================================
```

## 🛠️ 문제 해결

### 애플리케이션이 실행되지 않았을 때
```bash
# 확인
lsof -i :8080

# 실행
./gradlew bootRun
```

### 테스트 게시글이 없을 때
```bash
# 게시글 ID를 변경하거나
TEST_POST_ID=2 bash src/main/java/com/homesweet/homesweetback/common/k6/community/run-community-test.sh

# API로 게시글을 먼저 생성하세요
```

### k6가 설치되지 않았을 때
```bash
# macOS
brew install k6

# 기타 OS는 https://k6.io/docs/get-started/installation/ 참고
```

## 💡 팁

### CI/CD 통합
```yaml
# .github/workflows/performance-test.yml
- name: Run k6 Community Test
  run: |
    ./gradlew bootRun &
    sleep 30
    bash src/main/java/com/homesweet/homesweetback/common/k6/community/run-community-test.sh
```

### 빠른 검증만 원할 때
k6를 직접 실행하되 시간을 줄일 수 있습니다:
```bash
# 각 시나리오의 duration을 줄여서 빠르게 테스트
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e TEST_POST_ID=1 \
  src/main/java/com/homesweet/homesweetback/common/k6/community/community-full-test.js
```

## 📁 파일 구조

```
src/main/java/com/homesweet/homesweetback/common/
├── k6/
│   └── community/
│       ├── community-full-test.js      # 커뮤니티 전체 기능 테스트
│       ├── run-community-test.sh       # 실행 스크립트 (이거 하나면 됨!)
│       └── README.md                   # 이 파일
└── community-test-results.json         # 테스트 결과 (자동 생성)
```

## 🎯 핵심 포인트

1. **단 하나의 명령어로 모든 것을 테스트**: `bash src/main/java/com/homesweet/homesweetback/common/k6/community/run-community-test.sh`
2. **13분 안에 완료**: 커뮤니티의 모든 기능 검증
3. **동시성 제어 검증**: 100명이 각각 10번씩 좋아요(총 1000회)를 눌러도 데이터 정합성 유지
4. **실제 사용 패턴**: 사용자가 실제로 사용하는 것처럼 시뮬레이션
5. **명확한 성공/실패 기준**: 자동으로 체크하고 리포트
6. **모든 API 메트릭 수집**: 에러율, 응답 시간, 성공률 등 상세 분석

## 🔧 최근 개선 사항

### 동시성 테스트 강화
- **변경 전**: 100명이 총 100회를 나눠서 실행 (평균 1번씩)
- **변경 후**: 100명이 각각 10번씩 실행 (총 1000회) ✅
- `executor: 'per-vu-iterations'`로 변경하여 진정한 동시성 테스트 수행

### 메트릭 수집 개선
- 모든 API 호출에 일관된 `check()` 및 에러 카운터 추가
- 댓글 수정/삭제 에러 메트릭 추가
- 스트레스 테스트에도 성공률 측정 추가
- Threshold에 누락된 메트릭 모두 추가

### 인증 처리 개선
- 빈 Authorization 헤더가 전송되는 문제 수정
- AUTH_TOKEN이 없을 때 헤더 자체를 생략하도록 개선

### 결과 저장 경로 명확화
- 결과 파일 경로를 프로젝트 구조에 맞게 조정
- `RESULT_PATH` 환경 변수로 커스터마이징 가능

이게 전부입니다! 🚀
