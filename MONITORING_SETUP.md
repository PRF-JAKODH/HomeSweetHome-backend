# 🎉 모니터링 설정 완료!

Prometheus + Grafana + Jaeger 모니터링 시스템이 성공적으로 설정되었습니다.

## ✅ 설치 완료 항목

- [x] Spring Boot Actuator & Prometheus 의존성 추가
- [x] Micrometer Tracing (OpenTelemetry) 추가
- [x] Prometheus 설정 파일 생성
- [x] Docker Compose 모니터링 스택 구성
- [x] Jaeger OTLP 통합

## 🚀 시작하기

### 1. 모니터링 스택 실행 확인

```bash
docker-compose -f docker-compose.monitoring.yml ps
```

현재 실행 중인 서비스:
- ✅ Prometheus (http://localhost:9090)
- ✅ Grafana (http://localhost:3001)
- ✅ Jaeger (http://localhost:16686)

### 2. Spring Boot 애플리케이션 실행

```bash
./gradlew bootRun
```

또는

```bash
./gradlew build
java -jar build/libs/homesweet-back-V1.0.1.jar
```

### 3. 메트릭 확인

브라우저에서 접속:
```
http://localhost:8080/actuator/prometheus
```

Prometheus 형식의 메트릭이 보이면 성공!

## 📊 모니터링 도구 접속

| 도구 | URL | 계정 | 비고 |
|------|-----|------|------|
| Prometheus | http://localhost:9090 | - | 메트릭 쿼리 및 탐색 |
| Grafana | http://localhost:3001 | admin / admin | 대시보드 시각화 |
| Jaeger | http://localhost:16686 | - | 분산 트레이싱 |

## 🎨 Grafana 초기 설정

### 1단계: 로그인
- URL: http://localhost:3001
- ID: `admin`
- Password: `admin`

### 2단계: Prometheus 데이터소스 추가
1. 좌측 메뉴 > **Connections** > **Data sources**
2. **Add data source** 클릭
3. **Prometheus** 선택
4. URL 입력: `http://prometheus:9090`
5. **Save & Test** 클릭

### 3단계: Jaeger 데이터소스 추가
1. **Add data source** 클릭
2. **Jaeger** 선택
3. URL 입력: `http://jaeger:16686`
4. **Save & Test** 클릭

### 4단계: 대시보드 가져오기
1. 좌측 메뉴 > **Dashboards** > **New** > **Import**
2. 대시보드 ID 입력:
   - **4701** - JVM (Micrometer) ⭐ 추천
   - **11378** - Spring Boot Statistics
   - **12900** - Spring Boot 2.1 System Monitor
3. **Load** 클릭 후 Prometheus 데이터소스 선택
4. **Import** 클릭

## 🔍 빠른 테스트

### 1. API 호출해서 메트릭 생성
```bash
# 커뮤니티 게시글 조회
curl http://localhost:8080/api/v1/community/posts

# Health 체크
curl http://localhost:8080/actuator/health
```

### 2. Prometheus에서 확인
http://localhost:9090 접속 후 쿼리 실행:

```promql
# HTTP 요청 수
http_server_requests_seconds_count

# JVM 메모리 사용량
jvm_memory_used_bytes{area="heap"}
```

### 3. Jaeger에서 트레이싱 확인
1. http://localhost:16686 접속
2. Service: `homesweet-back` 선택
3. **Find Traces** 클릭
4. API 호출 흐름 확인

## 📈 주요 모니터링 메트릭

### API 성능
```promql
# 평균 응답시간
rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])

# 초당 요청 수 (RPS)
rate(http_server_requests_seconds_count[1m])

# 에러율
rate(http_server_requests_seconds_count{status=~"5.."}[5m])
```

### 시스템 리소스
```promql
# JVM Heap 사용률
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100

# DB 커넥션 풀 사용량
hikaricp_connections_active
```

## 🎯 커뮤니티 기능 모니터링 포인트

### N+1 문제 감지
Jaeger에서 `getPosts` 트레이스를 확인하여:
- 게시글 조회 쿼리: 1번
- 이미지 조회 쿼리: N번 (문제!)

### 조회수 증가 성능
```promql
# increaseViewCount API 응답시간
histogram_quantile(0.95,
  rate(http_server_requests_seconds_bucket{uri="/api/v1/community/posts/{postId}/views"}[5m])
)
```

## 🛠 문제 해결

### Spring Boot 메트릭이 안보여요
```bash
# 1. Actuator 엔드포인트 확인
curl http://localhost:8080/actuator/prometheus

# 2. Prometheus targets 확인
# http://localhost:9090/targets 에서 homesweet-backend 상태 확인
```

### Jaeger에 트레이스가 안보여요
```bash
# 1. OTLP 엔드포인트 확인
curl http://localhost:4318/v1/traces

# 2. application-dev.yml 설정 확인
# management.otlp.tracing.endpoint: http://localhost:4318/v1/traces
```

### Grafana 데이터소스 연결 실패
Docker 네트워크에서 접근하므로:
- ✅ `http://prometheus:9090` (올바름)
- ❌ `http://localhost:9090` (틀림)

## 🔄 모니터링 스택 관리

### 재시작
```bash
docker-compose -f docker-compose.monitoring.yml restart
```

### 로그 확인
```bash
docker-compose -f docker-compose.monitoring.yml logs -f prometheus
docker-compose -f docker-compose.monitoring.yml logs -f grafana
docker-compose -f docker-compose.monitoring.yml logs -f jaeger
```

### 중지
```bash
docker-compose -f docker-compose.monitoring.yml down
```

### 데이터까지 삭제
```bash
docker-compose -f docker-compose.monitoring.yml down -v
```

## 📚 상세 가이드

전체 설정 및 사용법은 [MONITORING_GUIDE.md](./MONITORING_GUIDE.md)를 참고하세요.

## 🎓 다음 단계

1. **커스텀 메트릭 추가**: 비즈니스 지표 수집
2. **알림 설정**: Grafana Alert 설정
3. **로그 수집**: Loki 추가 (옵션)
4. **성능 최적화**: 모니터링 데이터 기반 개선

---

Happy Monitoring! 🚀
