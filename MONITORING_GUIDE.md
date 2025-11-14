# 📊 모니터링 시스템 가이드

Prometheus + Grafana + Jaeger를 사용한 HomeSweetHome 백엔드 모니터링 가이드입니다.

## 🚀 빠른 시작

### 1. 의존성 설치
```bash
./gradlew build
```

### 2. 모니터링 스택 실행
```bash
# 모니터링 도구 실행
docker-compose -f docker-compose.monitoring.yml up -d

# 상태 확인
docker-compose -f docker-compose.monitoring.yml ps
```

### 3. Spring Boot 애플리케이션 실행
```bash
./gradlew bootRun
```

### 4. 모니터링 도구 접속

| 도구 | URL | 용도 | 기본 계정 |
|------|-----|------|-----------|
| **Prometheus** | http://localhost:9090 | 메트릭 수집 및 쿼리 | - |
| **Grafana** | http://localhost:3001 | 대시보드 시각화 | admin / admin |
| **Jaeger** | http://localhost:16686 | 분산 트레이싱 | - |

---

## 📈 Prometheus 사용법

### 메트릭 확인
1. http://localhost:9090 접속
2. 상단 검색창에서 메트릭 검색

### 자주 사용하는 메트릭

```promql
# HTTP 요청 수 (최근 5분)
rate(http_server_requests_seconds_count[5m])

# API 응답 시간 (평균)
rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])

# JVM 메모리 사용량
jvm_memory_used_bytes

# JVM Heap 사용률
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100

# DB 커넥션 풀 사용량
hikaricp_connections_active

# 커뮤니티 게시글 조회 API 응답시간
http_server_requests_seconds_sum{uri="/api/v1/community/posts/{postId}"} / http_server_requests_seconds_count{uri="/api/v1/community/posts/{postId}"}

# 에러율 (5xx 에러)
rate(http_server_requests_seconds_count{status=~"5.."}[5m])
```

### Targets 상태 확인
- Status > Targets 메뉴에서 Spring Boot 앱 연결 확인
- `homesweet-backend` job이 UP 상태여야 함

---

## 🎨 Grafana 설정

### 1. 초기 로그인
- URL: http://localhost:3001
- ID: `admin`
- PW: `admin`
- 최초 로그인시 비밀번호 변경 권장

### 2. Prometheus 데이터소스 추가

1. **좌측 메뉴 > Connections > Data sources**
2. **Add data source** 클릭
3. **Prometheus** 선택
4. 설정:
   ```
   Name: Prometheus
   URL: http://prometheus:9090
   ```
5. **Save & Test** 클릭

### 3. Jaeger 데이터소스 추가

1. **Add data source** 클릭
2. **Jaeger** 선택
3. 설정:
   ```
   Name: Jaeger
   URL: http://jaeger:16686
   ```
4. **Save & Test** 클릭

### 4. Spring Boot 대시보드 가져오기

#### 방법 1: 커뮤니티 대시보드 사용 (추천)

1. **좌측 메뉴 > Dashboards**
2. **New > Import**
3. 대시보드 ID 입력:
   - **4701** - JVM (Micrometer)
   - **11378** - Spring Boot Statistics
   - **12900** - Spring Boot 2.1 System Monitor
4. **Load** 클릭
5. Prometheus 데이터소스 선택
6. **Import** 클릭

#### 방법 2: 커스텀 대시보드 생성

1. **New > Dashboard**
2. **Add visualization** 클릭
3. 예시 패널:

**API 응답 시간 (평균)**
```promql
rate(http_server_requests_seconds_sum{application="homesweet-back"}[5m])
/
rate(http_server_requests_seconds_count{application="homesweet-back"}[5m])
```

**요청 처리량 (RPS)**
```promql
sum(rate(http_server_requests_seconds_count{application="homesweet-back"}[1m])) by (uri)
```

**JVM Heap 메모리 사용량**
```promql
jvm_memory_used_bytes{application="homesweet-back", area="heap"}
```

**DB 커넥션 풀 상태**
```promql
hikaricp_connections_active{application="homesweet-back"}
```

**에러율**
```promql
sum(rate(http_server_requests_seconds_count{application="homesweet-back", status=~"5.."}[5m]))
/
sum(rate(http_server_requests_seconds_count{application="homesweet-back"}[5m])) * 100
```

### 5. 알림 설정 (옵션)

1. **Alerting > Alert rules**
2. **New alert rule** 클릭
3. 예시: API 응답시간 알림
   ```
   Query: rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])
   Condition: WHEN avg() OF query(A, 5m) IS ABOVE 1
   ```

---

## 🔍 Jaeger 사용법

### 트레이스 조회

1. http://localhost:16686 접속
2. **Service** 드롭다운에서 `homesweet-back` 선택
3. **Find Traces** 클릭

### 트레이스 분석

- **Timeline View**: 요청 처리 흐름 시각화
- **Span Details**: 각 구간별 소요 시간
- **Tags/Logs**: 추가 메타데이터

### 유용한 필터

```
# 느린 요청 찾기
min duration: 1s

# 특정 API만 필터링
Tags: http.url=/api/v1/community/posts

# 에러 발생 요청
Tags: error=true
```

### 커스텀 Span 추가 (개발자용)

```java
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.Span;

@Service
@RequiredArgsConstructor
public class CommunityPostService {

    private final Tracer tracer;

    public CommunityPostResponse getPost(Long postId) {
        // 커스텀 span 시작
        Span span = tracer.nextSpan().name("getPost.imageQuery").start();
        try {
            // 비즈니스 로직
            List<String> imageUrls = imageRepository.findByPostOrderByImageOrderAsc(post)
                .stream()
                .map(CommunityImageEntity::getImageUrl)
                .toList();

            span.tag("post.id", postId.toString());
            span.tag("image.count", String.valueOf(imageUrls.size()));

            return CommunityPostResponse.from(post, imageUrls);
        } finally {
            span.end();
        }
    }
}
```

---

## 🎯 주요 모니터링 포인트

### 1. 커뮤니티 기능 성능

#### N+1 문제 감지
- Jaeger에서 `getPosts` 트레이스 확인
- 이미지 조회 쿼리가 게시글 수만큼 발생하는지 확인

#### 조회수 증가 병목 확인
- `increaseViewCount` API 응답시간 모니터링
- Lock 대기시간이 긴지 확인

### 2. 시스템 리소스

```promql
# CPU 사용률
process_cpu_usage{application="homesweet-back"}

# 메모리 사용률
jvm_memory_used_bytes / jvm_memory_max_bytes * 100

# GC 실행 횟수
rate(jvm_gc_pause_seconds_count[1m])
```

### 3. 데이터베이스

```promql
# 커넥션 풀 사용률
hikaricp_connections_active / hikaricp_connections_max * 100

# 커넥션 대기 시간
hikaricp_connections_acquire_seconds_sum / hikaricp_connections_acquire_seconds_count
```

### 4. API 성능

```promql
# 가장 느린 API Top 5
topk(5,
  rate(http_server_requests_seconds_sum[5m])
  /
  rate(http_server_requests_seconds_count[5m])
)

# 에러가 많은 API
topk(5, rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
```

---

## 🔧 트러블슈팅

### Spring Boot 메트릭이 안보여요

1. 애플리케이션 실행 확인
   ```bash
   curl http://localhost:8080/actuator/prometheus
   ```

2. Prometheus targets 확인
   - http://localhost:9090/targets
   - `homesweet-backend` 상태가 UP인지 확인

3. Mac에서 `host.docker.internal` 안되면:
   ```yaml
   # prometheus.yml 수정
   - targets: ['host.docker.internal:8080']
   # 또는
   - targets: ['172.17.0.1:8080']  # Docker 기본 게이트웨이
   ```

### Jaeger에 트레이스가 안보여요

1. OTLP endpoint 연결 확인
   ```bash
   curl http://localhost:4318/v1/traces
   ```

2. application-dev.yml 설정 확인
   ```yaml
   management:
     otlp:
       tracing:
         endpoint: http://localhost:4318/v1/traces
   ```

3. 로그에서 에러 확인
   ```bash
   # Spring Boot 로그에서 "otlp" 또는 "tracing" 검색
   ```

### Grafana에서 데이터가 안보여요

1. 데이터소스 연결 테스트
   - Connections > Data sources > Prometheus
   - "Save & Test" 클릭하여 연결 확인

2. 시간 범위 확인
   - 우측 상단 시간 선택기에서 "Last 15 minutes" 선택

3. 쿼리 확인
   - Query inspector로 실제 PromQL 쿼리 확인

---

## 🛑 모니터링 중지

```bash
# 모니터링 스택 중지
docker-compose -f docker-compose.monitoring.yml down

# 데이터까지 모두 삭제
docker-compose -f docker-compose.monitoring.yml down -v
```

---

## 📚 참고 자료

- [Prometheus 문서](https://prometheus.io/docs/)
- [Grafana 문서](https://grafana.com/docs/)
- [Jaeger 문서](https://www.jaegertracing.io/docs/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer 문서](https://micrometer.io/docs)

---

## 💡 다음 단계

1. **알림 설정**: Grafana Alert를 Slack/Discord와 연동
2. **로그 수집**: Loki + Promtail 추가
3. **성능 최적화**: 모니터링 데이터 기반 병목 지점 개선
4. **커스텀 메트릭**: 비즈니스 메트릭 추가 (게시글 작성 수, 좋아요 수 등)

---

**문의사항이나 이슈가 있으면 GitHub Issues에 등록해주세요!**
