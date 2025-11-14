# 🖥️ Node Exporter + cAdvisor 가이드

시스템 레벨 메트릭(CPU, 메모리, 디스크, 네트워크)과 Docker 컨테이너 메트릭을 수집하는 가이드입니다.

## 🚀 빠른 시작

### 1단계: Node Exporter 및 cAdvisor 시작
```bash
# 기존 모니터링 스택 재시작
docker-compose -f docker-compose.monitoring.yml down
docker-compose -f docker-compose.monitoring.yml up -d
```

### 2단계: 확인
```bash
# 실행 중인 컨테이너 확인
docker-compose -f docker-compose.monitoring.yml ps

# Node Exporter 메트릭 확인
curl http://localhost:9100/metrics | head -20

# cAdvisor 웹 UI 확인
open http://localhost:8081
```

### 3단계: Prometheus에서 확인
```bash
# http://localhost:9090/targets
# node-exporter와 cadvisor가 UP 상태인지 확인
```

---

## 📊 추가된 서비스

| 서비스 | 포트 | 용도 | UI |
|--------|------|------|-----|
| **Node Exporter** | 9100 | 호스트 시스템 메트릭 | ❌ (메트릭만) |
| **cAdvisor** | 8081 | Docker 컨테이너 메트릭 | ✅ http://localhost:8081 |

---

## 🔍 Node Exporter 메트릭

### CPU 메트릭
```promql
# CPU 사용률 (%)
100 - (avg by (instance) (rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)

# CPU 코어별 사용률
rate(node_cpu_seconds_total{mode!="idle"}[5m])

# Load Average (1분, 5분, 15분)
node_load1
node_load5
node_load15
```

### 메모리 메트릭
```promql
# 메모리 사용률 (%)
(1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100

# 사용 중인 메모리 (GB)
(node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / 1024 / 1024 / 1024

# Swap 사용률
(node_memory_SwapTotal_bytes - node_memory_SwapFree_bytes) / node_memory_SwapTotal_bytes * 100
```

### 디스크 메트릭
```promql
# 디스크 사용률 (%)
(1 - (node_filesystem_avail_bytes / node_filesystem_size_bytes)) * 100

# 디스크 I/O (읽기)
rate(node_disk_read_bytes_total[5m])

# 디스크 I/O (쓰기)
rate(node_disk_written_bytes_total[5m])

# 디스크 I/O 대기 시간
rate(node_disk_io_time_seconds_total[5m])
```

### 네트워크 메트릭
```promql
# 네트워크 수신 (bytes/sec)
rate(node_network_receive_bytes_total[5m])

# 네트워크 송신 (bytes/sec)
rate(node_network_transmit_bytes_total[5m])

# 네트워크 에러
rate(node_network_receive_errs_total[5m])
rate(node_network_transmit_errs_total[5m])
```

### 시스템 정보
```promql
# 부팅 시간
node_boot_time_seconds

# 파일 디스크립터 사용량
node_filefd_allocated

# 컨텍스트 스위칭
rate(node_context_switches_total[5m])
```

---

## 🐳 cAdvisor 메트릭

### 컨테이너 CPU
```promql
# 컨테이너별 CPU 사용률
rate(container_cpu_usage_seconds_total{name!=""}[5m]) * 100

# Spring Boot 컨테이너 CPU
rate(container_cpu_usage_seconds_total{name="homesweet-app"}[5m]) * 100
```

### 컨테이너 메모리
```promql
# 컨테이너별 메모리 사용량 (MB)
container_memory_usage_bytes{name!=""} / 1024 / 1024

# 메모리 사용률
container_memory_usage_bytes / container_spec_memory_limit_bytes * 100

# Spring Boot 컨테이너 메모리
container_memory_usage_bytes{name="homesweet-app"} / 1024 / 1024
```

### 컨테이너 네트워크
```promql
# 컨테이너별 네트워크 수신
rate(container_network_receive_bytes_total{name!=""}[5m])

# 컨테이너별 네트워크 송신
rate(container_network_transmit_bytes_total{name!=""}[5m])
```

### 컨테이너 파일시스템
```promql
# 컨테이너별 디스크 사용량
container_fs_usage_bytes{name!=""}

# 컨테이너별 디스크 I/O
rate(container_fs_reads_bytes_total{name!=""}[5m])
rate(container_fs_writes_bytes_total{name!=""}[5m])
```

---

## 🎨 Grafana 대시보드 설정

### 1. Node Exporter 대시보드

**Import 방법:**
1. Grafana 접속: http://localhost:3001
2. **Dashboards** > **Import**
3. 대시보드 ID 입력: **1860** (Node Exporter Full)
4. Prometheus 데이터소스 선택
5. **Import**

**확인 내용:**
- 📊 CPU 사용률 및 로드
- 💾 메모리 사용량
- 💿 디스크 I/O 및 사용률
- 🌐 네트워크 트래픽
- ⏱️ 시스템 업타임

### 2. cAdvisor 대시보드

**Import 방법:**
1. **Dashboards** > **Import**
2. 대시보드 ID 입력: **14282** (Docker Container & Host Metrics)
3. Prometheus 데이터소스 선택
4. **Import**

**확인 내용:**
- 🐳 컨테이너별 리소스 사용량
- 📈 컨테이너 CPU/메모리 추이
- 🌐 컨테이너 네트워크 트래픽
- 💿 컨테이너 디스크 I/O

### 3. 통합 대시보드 (직접 생성)

**패널 예시:**

#### 패널 1: 시스템 리소스 Overview
```promql
# CPU 사용률
100 - (avg(rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)

# 메모리 사용률
(1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100

# 디스크 사용률
(1 - (node_filesystem_avail_bytes{mountpoint="/"} / node_filesystem_size_bytes{mountpoint="/"})) * 100
```

#### 패널 2: Spring Boot + 시스템 통합
```promql
# JVM Heap vs 시스템 메모리
jvm_memory_used_bytes{area="heap"}
node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes

# API 요청 vs CPU 사용률
rate(http_server_requests_seconds_count[1m])
100 - (avg(rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)
```

---

## 🔥 부하 테스트 중 모니터링

### 시나리오: k6 부하 테스트 + 시스템 모니터링

**1단계: 부하 테스트 시작**
```bash
k6 run --vus 100 --duration 5m k6-tests/stress-test.js
```

**2단계: Grafana에서 실시간 확인**

**CPU 모니터링:**
```promql
# 시스템 CPU 사용률
100 - (avg(rate(node_cpu_seconds_total{mode="idle"}[1m])) * 100)

# Spring Boot 프로세스 CPU
process_cpu_usage{application="homesweet-back"}
```

**메모리 모니터링:**
```promql
# 시스템 메모리 사용률
(1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100

# JVM Heap 사용률
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100
```

**네트워크 모니터링:**
```promql
# 시스템 네트워크 수신
rate(node_network_receive_bytes_total[1m])

# HTTP 요청 수
rate(http_server_requests_seconds_count[1m])
```

**3단계: 병목 지점 분석**

만약 다음과 같은 증상이 보이면:

**CPU 100% 도달:**
```promql
# CPU가 병목인지 확인
100 - (avg(rate(node_cpu_seconds_total{mode="idle"}[1m])) * 100) > 90
```
→ **해결:** 코드 최적화, 스케일 아웃

**메모리 부족:**
```promql
# 메모리가 90% 이상 사용 중
(1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100 > 90
```
→ **해결:** JVM Heap 크기 조정, 메모리 누수 확인

**디스크 I/O 과부하:**
```promql
# I/O 대기 시간 증가
rate(node_disk_io_time_seconds_total[1m]) > 0.8
```
→ **해결:** 쿼리 최적화, 인덱스 추가, SSD 사용

---

## 🎯 알림 설정

### Grafana Alert 예시

**CPU 과부하 알림:**
```
Alert: CPU 사용률 90% 초과
Query: 100 - (avg(rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)
Condition: WHEN avg() IS ABOVE 90
For: 5m
```

**메모리 부족 알림:**
```
Alert: 메모리 사용률 85% 초과
Query: (1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100
Condition: WHEN avg() IS ABOVE 85
For: 3m
```

**디스크 용량 부족 알림:**
```
Alert: 디스크 사용률 90% 초과
Query: (1 - (node_filesystem_avail_bytes{mountpoint="/"} / node_filesystem_size_bytes{mountpoint="/"})) * 100
Condition: WHEN avg() IS ABOVE 90
```

---

## 🛠 문제 해결

### Node Exporter가 안보여요

```bash
# 1. 컨테이너 상태 확인
docker ps | grep node-exporter

# 2. 로그 확인
docker logs homesweet-node-exporter

# 3. 메트릭 직접 확인
curl http://localhost:9100/metrics | grep node_cpu

# 4. Prometheus targets 확인
curl http://localhost:9090/api/v1/targets | jq '.data.activeTargets[] | select(.labels.job=="node-exporter")'
```

### cAdvisor가 안보여요 (Mac M1/M2)

Mac에서는 cAdvisor가 제대로 작동하지 않을 수 있습니다.

**해결 방법:**
```yaml
# docker-compose.monitoring.yml에서 cAdvisor 제거 또는
# Docker Desktop에서 기본 제공하는 메트릭 사용

# 또는 Linux 이미지 사용
cadvisor:
  image: gcr.io/cadvisor/cadvisor:v0.47.0
  platform: linux/amd64  # 추가
```

### 메트릭이 수집되지 않아요

```bash
# Prometheus 설정 리로드
curl -X POST http://localhost:9090/-/reload

# 또는 Prometheus 재시작
docker-compose -f docker-compose.monitoring.yml restart prometheus
```

---

## 📊 실전 예시

### 부하 테스트 전후 비교

**Before (부하 테스트 전):**
```promql
# CPU: 5%
100 - (avg(rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)
→ 5.2%

# 메모리: 30%
(1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100
→ 32.8%
```

**During (부하 테스트 중 - 100 VU):**
```promql
# CPU: 45%
→ 47.3%

# 메모리: 55%
→ 58.1%

# 네트워크 수신: 5MB/s
rate(node_network_receive_bytes_total[1m])
→ 5,242,880 bytes/s
```

**Analysis:**
- ✅ CPU 여유 있음 (50% 미만)
- ✅ 메모리 여유 있음 (60% 미만)
- ✅ 네트워크 정상
- ✅ 스케일 아웃 필요 없음

---

## 📚 참고 자료

- [Node Exporter GitHub](https://github.com/prometheus/node_exporter)
- [cAdvisor GitHub](https://github.com/google/cadvisor)
- [Grafana Dashboard 1860](https://grafana.com/grafana/dashboards/1860)
- [Grafana Dashboard 14282](https://grafana.com/grafana/dashboards/14282)

---

## 🎉 완성!

이제 다음을 모니터링할 수 있습니다:

✅ **애플리케이션**: Spring Boot Actuator
✅ **시스템**: Node Exporter (CPU, 메모리, 디스크, 네트워크)
✅ **컨테이너**: cAdvisor (Docker 리소스)
✅ **부하 테스트**: k6
✅ **트레이싱**: Jaeger

**완벽한 Full-Stack 모니터링 시스템!** 🚀

---

**Happy Monitoring! 📊**
