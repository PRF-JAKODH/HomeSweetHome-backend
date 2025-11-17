# 🔍 관측성(Observability) 가이드

Grafana Loki, Jaeger, Prometheus를 사용한 로컬 환경 모니터링 완벽 가이드

## 📊 구성 요소

| 도구 | 용도 | 포트 | URL |
|-----|------|------|-----|
| **Grafana** | 통합 대시보드 | 3001 | http://localhost:3001 |
| **Loki** | 로그 수집/저장 | 3100 | http://localhost:3100 |
| **Jaeger** | 분산 추적 | 16686 | http://localhost:16686 |
| **Prometheus** | 메트릭 수집 | 9090 | http://localhost:9090 |
| **Promtail** | 로그 수집 에이전트 | 9080 | - |

---

## 🚀 빠른 시작 (3단계)

### 1️⃣ 모니터링 스택 실행

```bash
# 모든 모니터링 도구 시작
docker-compose -f docker-compose.monitoring.yml up -d

# 컨테이너 확인
docker ps | grep homesweet
```

**실행되는 서비스:**
- ✅ Grafana (대시보드)
- ✅ Loki (로그 저장소)
- ✅ Promtail (로그 수집)
- ✅ Jaeger (분산 추적)
- ✅ Prometheus (메트릭)
- ✅ Node Exporter (시스템 메트릭)
- ✅ cAdvisor (컨테이너 메트릭)

### 2️⃣ Spring Boot 애플리케이션 실행

```bash
# 애플리케이션 시작
./gradlew bootRun

# 로그 파일 생성 확인
ls -la logs/
# application.log 파일이 생성됩니다
```

### 3️⃣ 대시보드 접속

```bash
# Grafana 대시보드
open http://localhost:3001
# 로그인: admin / admin

# Jaeger UI
open http://localhost:16686
```

---

## 📝 Grafana Loki 사용법

### Loki란?
- Prometheus와 유사하지만 **로그 전용**
- 로그를 인덱싱하지 않고 라벨만 인덱싱 (저장 공간 효율적)
- Grafana와 완벽 통합

### Grafana에서 Loki 데이터 소스 추가

1. **Grafana 접속**: http://localhost:3001 (admin/admin)

2. **Configuration > Data Sources > Add data source**

3. **Loki 선택** 후 설정:
   ```
   Name: Loki
   URL: http://loki:3100
   ```

4. **Save & Test** 클릭

### 로그 쿼리 예제

#### 기본 쿼리
```logql
# 모든 로그
{job="homesweet-backend"}

# ERROR 로그만
{job="homesweet-backend", level="ERROR"}

# 특정 로거
{job="homesweet-backend", logger=~".*CommunityController.*"}
```

#### 고급 쿼리
```logql
# "Exception" 포함된 로그
{job="homesweet-backend"} |= "Exception"

# SQL 쿼리 로그
{job="homesweet-backend"} |= "SQL" | json

# 최근 5분간 ERROR 수
count_over_time({level="ERROR"}[5m])

# HTTP 요청 로그 (community API)
{job="homesweet-backend"} |~ "/api/v1/community.*"
```

### Loki 대시보드 만들기

1. **Dashboards > New Dashboard > Add visualization**

2. **Data source**: Loki 선택

3. **쿼리 입력**:
   ```logql
   {job="homesweet-backend", level="ERROR"}
   ```

4. **패널 제목 설정**: "에러 로그"

5. **Save dashboard**

---

## 🔬 Jaeger 사용법

### Jaeger란?
- **분산 추적(Distributed Tracing)** 시스템
- API 요청의 전체 흐름을 추적
- 병목 지점 식별

### Jaeger UI 사용

1. **Jaeger 접속**: http://localhost:16686

2. **Service 선택**: `homesweet-back`

3. **Operation 선택**:
   - `GET /api/v1/community/posts`
   - `POST /api/v1/community/posts/{id}/views`

4. **Find Traces** 클릭

### Trace 분석 예제

#### 느린 API 찾기

```
Service: homesweet-back
Tags: http.status_code=200
Min Duration: 1s

→ 1초 이상 걸린 요청만 조회
```

#### 에러 발생한 요청 찾기

```
Service: homesweet-back
Tags: error=true

→ 에러 발생한 모든 요청
```

### Trace 정보 읽는 법

```
Trace 타임라인:
├─ http-nio-8080-exec-1 [200ms]        # HTTP 요청 처리
│  ├─ CommunityPostService.getPost [150ms]
│  │  ├─ JPA SELECT query [100ms]      # ← 병목!
│  │  └─ 비즈니스 로직 [50ms]
│  └─ Response 생성 [50ms]
```

**분석:**
- 전체 200ms 중 JPA 쿼리가 100ms (50%)
- 쿼리 최적화 필요!

---

## 📈 실전 사용 시나리오

### 시나리오 1: 느린 API 디버깅

**문제**: `/api/v1/community/posts` 응답이 느림

**해결 과정:**

1. **Jaeger에서 Trace 확인**
   ```
   Service: homesweet-back
   Operation: GET /api/v1/community/posts
   Min Duration: 2s
   ```

2. **Trace 상세 보기**
   - DB 쿼리: 1.8s ← 문제 발견!
   - 비즈니스 로직: 0.2s

3. **Loki에서 SQL 로그 확인**
   ```logql
   {job="homesweet-backend"} |= "SELECT" |= "community_post"
   ```

4. **문제 해결**
   - N+1 쿼리 발견
   - Fetch Join 적용
   - 1.8s → 0.3s 개선!

---

### 시나리오 2: 에러 발생 원인 파악

**문제**: 사용자가 500 에러 신고

**해결 과정:**

1. **Loki에서 ERROR 로그 검색**
   ```logql
   {job="homesweet-backend", level="ERROR"}
   | json
   | line_format "{{.timestamp}} {{.logger}} {{.message}}"
   ```

2. **에러 스택 트레이스 확인**
   ```
   NullPointerException at CommunityPostService.java:45
   ```

3. **Jaeger에서 해당 요청 Trace 찾기**
   - Request ID로 검색
   - 어느 단계에서 실패했는지 확인

4. **원인 파악 및 수정**

---

### 시나리오 3: 부하 테스트 모니터링

**k6 테스트 중 모니터링**

```bash
# Terminal 1: 부하 테스트 실행
k6 run k6-tests/community-local-load.js

# Terminal 2: Loki에서 실시간 로그 확인
```

**Grafana에서 실시간 모니터링:**

1. **Loki 패널**: 에러 로그 모니터링
   ```logql
   rate({level="ERROR"}[1m])
   ```

2. **Prometheus 패널**: 응답 시간
   ```promql
   histogram_quantile(0.95,
     rate(http_server_requests_seconds_bucket[1m])
   )
   ```

3. **Jaeger**: 느린 요청 추적

---

## 🎨 추천 Grafana 대시보드

### 대시보드 1: 애플리케이션 Overview

**Row 1: 메트릭 (Prometheus)**
- QPS (초당 요청 수)
- 평균 응답 시간
- 에러율

**Row 2: 로그 (Loki)**
- ERROR 로그 카운트
- 최근 에러 로그 테이블

**Row 3: 트레이싱 (Jaeger 링크)**
- Jaeger UI로 바로 이동 버튼

### 대시보드 2: 커뮤니티 API 모니터링

**Loki 쿼리:**
```logql
# 게시글 조회 요청
{job="homesweet-backend"}
  |~ "/api/v1/community/posts"
  | json
  | line_format "{{.method}} {{.uri}} {{.status}} {{.duration}}ms"

# 조회수 증가 실패
{job="homesweet-backend"}
  |= "increaseViewCount"
  |= "ERROR"
```

---

## 🛠 문제 해결

### ❌ Loki에 로그가 안 보임

**원인 1**: Promtail이 로그 파일을 못 찾음

```bash
# 로그 파일 확인
ls -la logs/application.log

# Promtail 로그 확인
docker logs homesweet-promtail

# 해결: 애플리케이션 재시작
./gradlew bootRun
```

**원인 2**: Promtail 설정 오류

```bash
# Promtail 재시작
docker-compose -f docker-compose.monitoring.yml restart promtail
```

---

### ❌ Jaeger에 Trace가 안 보임

**확인 사항:**

```bash
# 1. Jaeger 컨테이너 상태
docker ps | grep jaeger

# 2. 애플리케이션 설정 확인
grep -A 5 "tracing" src/main/resources/application-dev.yml

# 3. API 요청 보내기 (Trace 생성)
curl http://localhost:8080/api/v1/community/posts
```

**원인**: sampling.probability가 0
```yaml
# application-dev.yml
management:
  tracing:
    sampling:
      probability: 1.0  # ← 1.0으로 설정 (100% 추적)
```

---

### ❌ Grafana에 Loki 데이터 소스 추가 실패

**에러**: "Data source connection failed"

**해결:**
```yaml
# docker-compose.monitoring.yml에서 확인
# Grafana와 Loki가 같은 네트워크에 있어야 함

URL: http://loki:3100  # ✅ 컨테이너명 사용
URL: http://localhost:3100  # ❌ localhost는 안됨
```

---

## 📚 유용한 LogQL 쿼리 모음

### 성능 모니터링
```logql
# 응답 시간 2초 이상인 요청
{job="homesweet-backend"}
  | regexp "duration=(?P<duration>\\d+)"
  | duration > 2000

# DB 쿼리 실행 로그
{job="homesweet-backend"} |= "Hibernate:" |= "select"
```

### 에러 분석
```logql
# Exception 종류별 카운트
sum by (exception) (
  count_over_time({level="ERROR"} | json [1h])
)

# 특정 사용자 에러
{job="homesweet-backend"}
  |= "userId=123"
  |= "ERROR"
```

### 비즈니스 메트릭
```logql
# 게시글 작성 수
count_over_time(
  {job="homesweet-backend"}
    |= "createPost"
    |= "SUCCESS"
  [1h]
)

# 조회수 증가 실패율
sum(
  rate({job="homesweet-backend"}
    |= "increaseViewCount"
    |= "ERROR"
  [5m])
)
/
sum(
  rate({job="homesweet-backend"}
    |= "increaseViewCount"
  [5m])
)
```

---

## 🎯 다음 단계

### 1. Grafana 대시보드 Import

공식 대시보드 활용:
- **Loki Dashboard**: 12019
- **Spring Boot Dashboard**: 19004
- **JVM Dashboard**: 4701

```
Grafana > Dashboards > Import >
Dashboard ID 입력 > Load > Import
```

### 2. Alert 설정

```
Grafana > Alerting > Alert rules > New alert rule

조건: {level="ERROR"}
임계값: 분당 10건 이상
알림: Slack, Email 등
```

### 3. 로그 보관 정책

```yaml
# docker-compose.monitoring.yml
loki:
  # 로그 보관 기간 설정
  -limits_config.retention_period: 7d
```

---

## 📖 참고 자료

- [Loki LogQL 문법](https://grafana.com/docs/loki/latest/logql/)
- [Jaeger 가이드](https://www.jaegertracing.io/docs/)
- [OpenTelemetry with Spring Boot](https://opentelemetry.io/docs/instrumentation/java/automatic/)

---

**Happy Observing! 🔍**
