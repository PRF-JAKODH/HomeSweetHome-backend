# 🎨 Grafana 설정 완벽 가이드

Grafana에서 Loki, Jaeger, Prometheus를 설정하는 방법

---

## 🚀 시작 전 확인

```bash
# 1. 모니터링 스택이 실행 중인지 확인
docker ps | grep grafana
docker ps | grep loki
docker ps | grep jaeger

# 2. Grafana 접속
open http://localhost:3001
```

---

## 1️⃣ Grafana 최초 접속

### 로그인 화면

```
Username: admin
Password: admin
```

- 로그인 후 **비밀번호 변경 화면**이 나옵니다
- 변경해도 되고, **Skip** 클릭해도 됩니다 (로컬 환경이므로)

---

## 2️⃣ Loki 데이터 소스 추가

### 단계 1: Data Sources 메뉴 이동

**방법 1 (추천):**
```
좌측 메뉴 > ⚙️ Configuration (톱니바퀴) > Data sources
```

**방법 2:**
```
좌측 메뉴 > Connections > Data sources
```

### 단계 2: Add data source 클릭

화면 우측 상단의 **"Add data source"** 버튼 클릭

### 단계 3: Loki 선택

검색창에 `loki` 입력 → **Loki** 카드 클릭

### 단계 4: Loki 설정 입력

```yaml
Name: Loki
Default: ☐ (체크 안함)

# HTTP 섹션
URL: http://loki:3100

# Auth 섹션
Basic auth: ☐ (체크 안함)
모든 인증 옵션: OFF

# 기타 설정
모두 기본값으로 유지
```

⚠️ **중요**: URL은 반드시 `http://loki:3100` (컨테이너 이름 사용)

### 단계 5: Save & test

화면 맨 아래 **"Save & test"** 버튼 클릭

✅ 성공 메시지:
```
Data source connected and labels found.
```

❌ 실패 시:
```bash
# Loki 컨테이너 확인
docker ps | grep loki

# 로그 확인
docker logs homesweet-loki
```

---

## 3️⃣ Prometheus 데이터 소스 추가

### 단계 1: Add data source

`Data sources` 페이지에서 다시 **"Add data source"** 클릭

### 단계 2: Prometheus 선택

검색창에 `prometheus` 입력 → **Prometheus** 선택

### 단계 3: Prometheus 설정

```yaml
Name: Prometheus
Default: ☑️ (체크 - 기본 데이터 소스로 설정)

# HTTP
URL: http://prometheus:9090

# Scrape interval
Scrape interval: 15s (기본값)

# Query timeout
Query timeout: 60s (기본값)
```

### 단계 4: Save & test

✅ 성공 메시지:
```
Data source is working
```

---

## 4️⃣ 로그 확인하기 (Loki Explore)

### 단계 1: Explore 메뉴

좌측 메뉴 > **🔍 Explore** (나침반 아이콘)

### 단계 2: 데이터 소스 선택

상단 드롭다운에서 **Loki** 선택

### 단계 3: 첫 번째 쿼리 실행

**Label browser 사용 (쉬운 방법):**

1. "Label filters" 옆 **"+ Add filter"** 클릭
2. Label: `job`, Operator: `=`, Value: `homesweet-backend` 선택
3. **"Run query"** 버튼 클릭

**쿼리 직접 입력:**

```logql
{job="homesweet-backend"}
```

### 단계 4: 로그 확인

- 하단에 로그 라인들이 표시됩니다
- 각 로그 라인을 클릭하면 상세 정보 확인 가능

### 유용한 쿼리들

```logql
# ERROR 로그만
{job="homesweet-backend", level="ERROR"}

# 커뮤니티 API 로그
{job="homesweet-backend"} |~ "/api/v1/community.*"

# Exception 포함 로그
{job="homesweet-backend"} |= "Exception"

# SQL 쿼리 로그
{job="homesweet-backend"} |= "Hibernate:" |= "select"

# 최근 5분간 ERROR 수
sum(count_over_time({level="ERROR"}[5m]))
```

---

## 5️⃣ 대시보드 만들기

### 방법 1: 새 대시보드 생성

**단계 1: 대시보드 생성**
```
좌측 메뉴 > Dashboards > New > New Dashboard
```

**단계 2: 패널 추가**
```
"Add visualization" 클릭
```

**단계 3: 데이터 소스 선택**
```
Data source: Loki 선택
```

**단계 4: 쿼리 입력**

에러 로그 모니터링 패널:
```logql
sum(count_over_time({job="homesweet-backend", level="ERROR"}[1m]))
```

**단계 5: 패널 설정**
```
Panel title: 에러 로그 (분당)
Visualization: Time series (그래프)
```

**단계 6: Apply 클릭**

**단계 7: Save dashboard**
```
우측 상단 💾 아이콘 클릭
Dashboard name: HomeSweetHome Logs
```

---

### 방법 2: 공식 대시보드 Import (추천!)

**단계 1: Import 메뉴**
```
좌측 메뉴 > Dashboards > Import
```

**단계 2: 대시보드 ID 입력**

Spring Boot 대시보드:
```
Dashboard ID: 19004
Load 버튼 클릭
```

**단계 3: 데이터 소스 선택**
```
Prometheus: Prometheus (방금 추가한 것)
Import 버튼 클릭
```

**추천 대시보드 ID 목록:**

| ID | 이름 | 용도 |
|----|------|------|
| **19004** | Spring Boot 2.1 Statistics | Spring Boot 전체 메트릭 |
| **12019** | Loki Logs | Loki 로그 대시보드 |
| **4701** | JVM (Micrometer) | JVM 메모리, GC 등 |
| **15760** | Docker Container Monitoring | 컨테이너 리소스 |

---

## 6️⃣ 커스텀 대시보드 만들기 (실전)

### 예제: 커뮤니티 API 모니터링 대시보드

**Row 1: API 메트릭 (Prometheus)**

**패널 1: QPS (초당 요청 수)**
```promql
sum(rate(http_server_requests_seconds_count{uri=~"/api/v1/community.*"}[1m]))
```
- Visualization: Stat
- Unit: requests/sec

**패널 2: 평균 응답 시간**
```promql
rate(http_server_requests_seconds_sum{uri=~"/api/v1/community.*"}[1m])
/
rate(http_server_requests_seconds_count{uri=~"/api/v1/community.*"}[1m])
```
- Visualization: Time series
- Unit: seconds

**패널 3: 에러율**
```promql
sum(rate(http_server_requests_seconds_count{uri=~"/api/v1/community.*", status=~"5.."}[1m]))
/
sum(rate(http_server_requests_seconds_count{uri=~"/api/v1/community.*"}[1m]))
```
- Visualization: Gauge
- Unit: percentunit (0-1)
- Thresholds: Green(0-0.01), Yellow(0.01-0.05), Red(0.05-1)

---

**Row 2: 로그 (Loki)**

**패널 4: 에러 로그 카운트**
```logql
sum(count_over_time({job="homesweet-backend", level="ERROR"}[5m]))
```
- Visualization: Time series

**패널 5: 최근 에러 로그**
```logql
{job="homesweet-backend", level="ERROR"}
```
- Visualization: Logs
- Show: Last 50 lines

**패널 6: API별 요청 수**
```logql
sum by (uri) (
  count_over_time({job="homesweet-backend"}
    |~ "/api/v1/community.*" [5m])
)
```
- Visualization: Bar chart

---

## 7️⃣ Alert 설정 (선택사항)

### 에러 발생 시 알림 설정

**단계 1: Contact point 생성**
```
좌측 메뉴 > Alerting > Contact points > New contact point

Name: Email
Integration: Email
Addresses: your-email@example.com
```

**단계 2: Alert rule 생성**
```
Alerting > Alert rules > New alert rule

Alert rule name: High Error Rate
Data source: Loki

Query:
sum(rate({level="ERROR"}[5m])) > 10

Condition: WHEN last() OF query(A) IS ABOVE 10

Evaluate every: 1m
For: 5m
```

**단계 3: Contact point 연결**
```
Contact point: Email 선택
Save rule
```

---

## 8️⃣ 팁과 트릭

### 시간 범위 변경

우측 상단 시계 아이콘:
```
Last 5 minutes
Last 15 minutes
Last 1 hour
Last 6 hours
```

### 자동 새로고침

우측 상단 새로고침 아이콘:
```
5s, 10s, 30s, 1m 등
```

### 쿼리 결과 다운로드

패널 제목 클릭 > Inspect > Data > Download CSV

### 대시보드 공유

우측 상단 공유 아이콘:
```
Link 복사
Snapshot 생성 (외부 공유)
```

---

## 🛠 문제 해결

### "Data source connected but no labels found"

```bash
# 1. Promtail이 로그를 수집하고 있는지 확인
docker logs homesweet-promtail

# 2. 로그 파일 존재 확인
ls -la logs/application.log

# 3. Spring Boot 앱 재시작
./gradlew bootRun
```

### 패널에 "No data" 표시

**원인 1: 시간 범위 문제**
- 우측 상단에서 시간 범위를 "Last 1 hour"로 변경

**원인 2: 쿼리 오류**
- Explore에서 쿼리를 먼저 테스트

**원인 3: 데이터가 실제로 없음**
```bash
# API 요청을 보내서 로그/메트릭 생성
curl http://localhost:8080/api/v1/community/posts
```

### Grafana가 느림

```bash
# 불필요한 컨테이너 정리
docker system prune

# Grafana 재시작
docker-compose -f docker-compose.monitoring.yml restart grafana
```

---

## 📚 다음 단계

1. ✅ Loki 데이터 소스 추가 완료
2. ✅ Prometheus 데이터 소스 추가 완료
3. ✅ 대시보드 Import 완료
4. 🔄 커스텀 대시보드 만들기
5. 🔄 Alert 설정하기
6. 🔄 Jaeger UI 연동 (링크 패널 추가)

---

**설정 완료! 🎉**

다음 가이드: [OBSERVABILITY_GUIDE.md](./OBSERVABILITY_GUIDE.md)
