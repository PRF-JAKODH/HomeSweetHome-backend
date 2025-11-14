# 🎉 완전한 모니터링 시스템 구축 완료!

## ✅ 설치된 모니터링 스택

| 도구 | 포트 | 용도 | 상태 |
|------|------|------|------|
| **Prometheus** | 9090 | 메트릭 수집 | ✅ UP |
| **Grafana** | 3001 | 대시보드 시각화 | ✅ UP |
| **Jaeger** | 16686 | 분산 트레이싱 | ✅ UP |
| **Node Exporter** | 9100 | 시스템 메트릭 | ✅ UP |
| **cAdvisor** | 8081 | 컨테이너 메트릭 | ✅ UP |

---

## 🎯 모니터링 레이어

```
┌─────────────────────────────────────────────────────┐
│                 Grafana Dashboard                    │
│              http://localhost:3001                   │
└─────────────────────────────────────────────────────┘
                        ▲
                        │
┌─────────────────────────────────────────────────────┐
│                   Prometheus                         │
│              http://localhost:9090                   │
└─────────────────────────────────────────────────────┘
                        ▲
        ┌───────────────┼───────────────┬──────────────┐
        │               │               │              │
┌───────┴──────┐ ┌─────┴──────┐ ┌─────┴──────┐ ┌────┴─────┐
│ Spring Boot  │ │    Node    │ │  cAdvisor  │ │  Jaeger  │
│  Actuator    │ │  Exporter  │ │            │ │          │
│   :8080      │ │   :9100    │ │   :8081    │ │  :16686  │
└──────────────┘ └────────────┘ └────────────┘ └──────────┘
│                │                │              │
│ - API 성능    │ - CPU 사용률   │ - 컨테이너   │ - 분산
│ - JVM 메트릭  │ - 메모리       │   리소스     │   트레이싱
│ - DB 커넥션   │ - 디스크 I/O   │ - Docker     │ - API 흐름
│ - HTTP 요청   │ - 네트워크     │   메트릭     │ - 병목 분석
└───────────────┴────────────────┴──────────────┴───────────┘
```

---

## 🚀 빠른 시작

### 전체 확인
```bash
# 모든 서비스 상태 확인
docker-compose -f docker-compose.monitoring.yml ps

# 예상 결과:
# ✅ homesweet-prometheus      (UP)
# ✅ homesweet-grafana         (UP)
# ✅ homesweet-jaeger          (UP)
# ✅ homesweet-node-exporter   (UP)
# ✅ homesweet-cadvisor        (UP)
```

### 접속 URL
```bash
# Prometheus
open http://localhost:9090

# Grafana (admin/admin)
open http://localhost:3001

# Jaeger
open http://localhost:16686

# cAdvisor
open http://localhost:8081
```

---

## 📊 수집되는 메트릭

### 1. 애플리케이션 메트릭 (Spring Boot Actuator)
```promql
# API 응답시간
rate(http_server_requests_seconds_sum[1m]) / rate(http_server_requests_seconds_count[1m])

# 초당 요청 수 (RPS)
rate(http_server_requests_seconds_count[1m])

# JVM Heap 사용률
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100

# DB 커넥션 풀
hikaricp_connections_active

# 에러율
rate(http_server_requests_seconds_count{status=~"5.."}[1m])
```

### 2. 시스템 메트릭 (Node Exporter)
```promql
# CPU 사용률
100 - (avg(rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)

# 메모리 사용률
(1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100

# 디스크 사용률
(1 - (node_filesystem_avail_bytes{mountpoint="/"} / node_filesystem_size_bytes{mountpoint="/"})) * 100

# 네트워크 트래픽
rate(node_network_receive_bytes_total[1m])
rate(node_network_transmit_bytes_total[1m])
```

### 3. 컨테이너 메트릭 (cAdvisor)
```promql
# 컨테이너별 CPU 사용률
rate(container_cpu_usage_seconds_total{name!=""}[5m]) * 100

# 컨테이너별 메모리 사용량
container_memory_usage_bytes{name!=""} / 1024 / 1024

# 컨테이너별 네트워크
rate(container_network_receive_bytes_total{name!=""}[5m])
```

---

## 🎨 Grafana 대시보드 설정

### 1. 데이터소스 추가 (한번만)

```bash
# http://localhost:3001 접속 (admin/admin)
# Connections > Data sources > Add data source
```

**Prometheus:**
- Name: `Prometheus`
- URL: `http://prometheus:9090`
- Save & Test

**Jaeger:**
- Name: `Jaeger`
- URL: `http://jaeger:16686`
- Save & Test

### 2. 대시보드 Import

| ID | 이름 | 내용 |
|----|------|------|
| **4701** | JVM (Micrometer) | Spring Boot JVM 메트릭 |
| **11378** | Spring Boot Statistics | Spring Boot 통계 |
| **1860** | Node Exporter Full | 시스템 전체 메트릭 |
| **14282** | Docker Container & Host | 컨테이너 메트릭 |

**Import 방법:**
```
Dashboards > New > Import > Dashboard ID 입력 > Load > Import
```

---

## 🔍 실전 사용법

### 시나리오 1: 부하 테스트 중 전체 모니터링

**1단계: 부하 테스트 시작**
```bash
k6 run --vus 100 --duration 5m k6-tests/community-load-test.js
```

**2단계: Grafana에서 확인**

**패널 1: API 성능**
```promql
# 응답시간 p95
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[1m]))

# 처리량 (RPS)
sum(rate(http_server_requests_seconds_count[1m]))
```

**패널 2: 시스템 리소스**
```promql
# CPU 사용률
100 - (avg(rate(node_cpu_seconds_total{mode="idle"}[1m])) * 100)

# 메모리 사용률
(1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100
```

**패널 3: 컨테이너 리소스**
```promql
# Spring Boot 컨테이너 CPU
rate(container_cpu_usage_seconds_total{name=~".*homesweet.*"}[1m]) * 100

# Spring Boot 컨테이너 메모리
container_memory_usage_bytes{name=~".*homesweet.*"} / 1024 / 1024
```

**3단계: Jaeger에서 병목 분석**
- http://localhost:16686
- Service: `homesweet-back`
- 느린 요청 찾기
- N+1 문제 확인

---

### 시나리오 2: N+1 문제 발견 및 해결

**Before (문제 있음):**
```bash
# 1. 스파이크 테스트 실행
k6 run k6-tests/spike-test.js

# 2. Jaeger 확인
# Operation: GET /api/v1/community/posts
# Span 개수: 21개 (1 게시글 조회 + 20 이미지 조회) ❌

# 3. Prometheus 확인
histogram_quantile(0.95,
  rate(http_server_requests_seconds_bucket{uri="/api/v1/community/posts"}[1m])
)
# 결과: 150ms ❌
```

**코드 수정 (Fetch Join):**
```java
@EntityGraph(attributePaths = {"images"})
Page<CommunityPostEntity> findByIsDeletedFalse(Pageable pageable);
```

**After (해결됨):**
```bash
# 1. 다시 테스트
k6 run k6-tests/spike-test.js

# 2. Jaeger 확인
# Span 개수: 2개 (1 게시글 조회 + 1 이미지 Fetch Join) ✅

# 3. Prometheus 확인
# 결과: 45ms ✅ (70% 개선!)
```

---

### 시나리오 3: 시스템 리소스 병목 확인

**증상:**
```bash
# 부하 테스트 중 응답시간 급증
k6 run k6-tests/stress-test.js
# p(95): 2000ms ❌
```

**원인 분석:**

**1. CPU 확인**
```promql
100 - (avg(rate(node_cpu_seconds_total{mode="idle"}[1m])) * 100)
# 결과: 95% ❌ (CPU 병목!)
```

**2. 메모리 확인**
```promql
(1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100
# 결과: 65% ✅ (여유 있음)
```

**3. 디스크 I/O 확인**
```promql
rate(node_disk_io_time_seconds_total[1m])
# 결과: 0.3 ✅ (정상)
```

**해결:**
- CPU가 병목 → 코드 최적화 또는 스케일 아웃 필요
- N+1 문제 해결
- 불필요한 계산 제거

---

## 🚨 알림 설정

### Grafana Alert 예시

**1. API 응답시간 초과**
```
Alert: API 응답시간 1초 초과
Query: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))
Condition: WHEN avg() IS ABOVE 1
For: 5m
Message: API 응답시간이 1초를 초과했습니다. 확인이 필요합니다.
```

**2. CPU 과부하**
```
Alert: CPU 사용률 90% 초과
Query: 100 - (avg(rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)
Condition: WHEN avg() IS ABOVE 90
For: 5m
```

**3. 메모리 부족**
```
Alert: 메모리 사용률 85% 초과
Query: (1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100
Condition: WHEN avg() IS ABOVE 85
For: 3m
```

**4. 에러율 급증**
```
Alert: 에러율 5% 초과
Query: rate(http_server_requests_seconds_count{status=~"5.."}[1m]) / rate(http_server_requests_seconds_count[1m]) * 100
Condition: WHEN avg() IS ABOVE 5
```

---

## 📚 전체 문서 목록

| 문서 | 내용 |
|------|------|
| [MONITORING_GUIDE.md](MONITORING_GUIDE.md) | Prometheus + Grafana + Jaeger 상세 가이드 |
| [MONITORING_SETUP.md](MONITORING_SETUP.md) | 초기 설정 완료 |
| [NODE_EXPORTER_GUIDE.md](NODE_EXPORTER_GUIDE.md) | Node Exporter + cAdvisor 가이드 |
| [K6_LOAD_TESTING.md](K6_LOAD_TESTING.md) | k6 부하 테스트 상세 가이드 |
| [QUICK_START_K6.md](QUICK_START_K6.md) | k6 빠른 시작 |
| [K6_PROMETHEUS_FIX.md](K6_PROMETHEUS_FIX.md) | k6 문제 해결 |
| [TEST_DATA_SETUP.md](TEST_DATA_SETUP.md) | 테스트 데이터 설정 |
| [MONITORING_COMPLETE.md](MONITORING_COMPLETE.md) | ⭐ 이 파일 (전체 요약) |

---

## 🎯 다음 단계

### 1. 성능 최적화
- [ ] N+1 문제 해결 (Fetch Join)
- [ ] 조회수 Redis 비동기 처리
- [ ] 게시글 목록 캐싱
- [ ] DB 인덱스 최적화

### 2. 모니터링 고도화
- [ ] 커스텀 Grafana 대시보드 생성
- [ ] Slack/Discord 알림 연동
- [ ] 로그 수집 (Loki 추가)
- [ ] 비즈니스 메트릭 추가

### 3. 부하 테스트 확장
- [ ] 인증 포함 부하 테스트
- [ ] 게시글 작성 동시성 테스트
- [ ] 장시간 안정성 테스트 (Soak Test)

### 4. CI/CD 통합
- [ ] GitHub Actions에 k6 추가
- [ ] PR마다 자동 부하 테스트
- [ ] 성능 회귀 방지 체크

---

## 🎉 완성!

**축하합니다!** 🎊

완벽한 Full-Stack 모니터링 시스템을 구축했습니다:

✅ **애플리케이션 모니터링**: Spring Boot Actuator
✅ **시스템 모니터링**: Node Exporter (CPU, 메모리, 디스크, 네트워크)
✅ **컨테이너 모니터링**: cAdvisor (Docker 리소스)
✅ **분산 트레이싱**: Jaeger (API 호출 흐름)
✅ **메트릭 수집**: Prometheus
✅ **시각화**: Grafana
✅ **부하 테스트**: k6

이제 다음을 할 수 있습니다:
- 🔍 실시간 성능 모니터링
- 📊 시스템 리소스 추적
- 🐛 병목 지점 빠른 발견
- 📈 성능 개선 효과 측정
- 🚨 문제 발생시 즉시 알림

---

**Happy Monitoring! 🚀**
