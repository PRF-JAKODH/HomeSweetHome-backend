# ⚡ k6 부하 테스트 빠른 시작

## 🚀 3분 안에 시작하기

### 1단계: 모니터링 스택 실행
```bash
docker-compose -f docker-compose.monitoring.yml up -d
```

### 2단계: 애플리케이션 실행
```bash
./gradlew bootRun
```

### 3단계: 부하 테스트 실행
```bash
# 기본 테스트 (모니터링 없이)
k6 run k6-tests/community-load-test.js

# Prometheus 통합 (추천!)
k6 run --out experimental-prometheus-rw k6-tests/community-load-test.js
```

### 4단계: 결과 확인
- **k6 결과**: 터미널에 즉시 표시
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3001 (admin/admin)
- **Jaeger**: http://localhost:16686

---

## 📊 테스트 종류

| 테스트 | 명령어 | 목적 |
|--------|--------|------|
| **Load Test** | `k6 run k6-tests/community-load-test.js` | 정상 트래픽 성능 |
| **Stress Test** | `k6 run k6-tests/stress-test.js` | 시스템 한계 찾기 |
| **Spike Test** | `k6 run k6-tests/spike-test.js` | 급격한 트래픽 대응 |

---

## 🎯 자주 사용하는 명령어

### 빠른 테스트
```bash
# 10명, 30초
k6 run --vus 10 --duration 30s k6-tests/community-load-test.js

# 100명, 5분
k6 run --vus 100 --duration 5m k6-tests/community-load-test.js
```

### HTML 리포트 생성 (추천!)
```bash
k6 run k6-tests/community-load-test-simple.js
# summary.html 파일이 생성됨
open summary.html
```

### ~~Prometheus 통합~~ (현재 미지원)
```bash
# ⚠️ Prometheus 3.x에서는 Remote Write가 기본적으로 비활성화되어 있습니다
# 대신 HTML 리포트를 사용하거나, Spring Boot 메트릭을 Prometheus에서 직접 확인하세요
```

### 결과 저장
```bash
k6 run --out json=results.json k6-tests/community-load-test.js
```

---

## 📈 Grafana 대시보드 설정 (1분)

1. http://localhost:3001 접속 (admin/admin)
2. **Connections** > **Data sources** > **Add data source**
3. **Prometheus** 선택
4. URL: `http://prometheus:9090`
5. **Save & Test**
6. **Dashboards** > **Import** > **2587** (k6 대시보드)

---

## 🔍 Prometheus 쿼리 예시

```promql
# k6 가상 사용자 수
k6_vus

# k6 요청 수
rate(k6_http_reqs_total[1m])

# k6 응답시간 p95
k6_http_req_duration{quantile="0.95"}

# 서버 응답시간
rate(http_server_requests_seconds_sum[1m]) / rate(http_server_requests_seconds_count[1m])
```

---

## 🛠 문제 해결

### k6 메트릭이 Prometheus에 안보여요
```bash
# Prometheus Remote Write 활성화 확인
docker logs homesweet-prometheus | grep "remote-write"

# k6 실행시 --out 옵션 확인
k6 run --out experimental-prometheus-rw k6-tests/community-load-test.js
```

### 애플리케이션이 응답하지 않아요
```bash
# Health 체크
curl http://localhost:8080/actuator/health

# Prometheus 메트릭 확인
curl http://localhost:8080/actuator/prometheus
```

---

## 📚 더 자세한 내용

전체 가이드: [K6_LOAD_TESTING.md](./K6_LOAD_TESTING.md)

---

**Happy Testing! 🎉**
