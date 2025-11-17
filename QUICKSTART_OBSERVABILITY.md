# ⚡ Loki & Jaeger 빠른 시작 가이드

로컬 환경에서 5분 안에 Grafana Loki와 Jaeger를 사용해보세요!

## 🎯 1단계: 모니터링 스택 실행 (1분)

```bash
# 모든 모니터링 도구 시작
docker-compose -f docker-compose.monitoring.yml up -d

# 실행 확인 (7개 컨테이너)
docker ps | grep homesweet

# 예상 출력:
# homesweet-grafana
# homesweet-loki
# homesweet-promtail
# homesweet-jaeger
# homesweet-prometheus
# homesweet-node-exporter
# homesweet-cadvisor
```

## 🎯 2단계: Spring Boot 앱 실행 (1분)

```bash
# 애플리케이션 시작
./gradlew bootRun

# 로그 파일 생성 확인
ls -la logs/application.log
# 파일이 있으면 성공!
```

## 🎯 3단계: Grafana에서 Loki 연결 (2분)

### 3-1. Grafana 접속
```bash
open http://localhost:3001
```
- 로그인: `admin` / `admin`
- 비밀번호 변경 (Skip 가능)

### 3-2. Loki 데이터 소스 추가

1. 좌측 메뉴 > **⚙️ Configuration** > **Data Sources**
2. **Add data source** 클릭
3. **Loki** 검색 후 선택
4. 설정 입력:
   ```
   Name: Loki
   URL: http://loki:3100
   ```
5. **Save & Test** 클릭 → "Data source connected" 확인

### 3-3. 로그 확인

1. 좌측 메뉴 > **🔍 Explore**
2. 데이터 소스: **Loki** 선택
3. 쿼리 입력:
   ```logql
   {job="homesweet-backend"}
   ```
4. **Run query** 클릭
5. 로그가 보이면 **성공!** 🎉

## 🎯 4단계: Jaeger로 API 추적 (1분)

### 4-1. API 요청 보내기

```bash
# 게시글 목록 조회 (Trace 생성)
curl http://localhost:8080/api/v1/community/posts
```

### 4-2. Jaeger UI에서 확인

```bash
open http://localhost:16686
```

1. **Service**: `homesweet-back` 선택
2. **Operation**: `GET /api/v1/community/posts` 선택
3. **Find Traces** 클릭
4. Trace 클릭하면 **상세 타임라인** 확인 가능!

---

## ✅ 확인 사항

### Loki 작동 확인
```logql
# Grafana Explore에서 실행
{job="homesweet-backend", level="INFO"}
```
→ 로그가 보이면 OK!

### Jaeger 작동 확인
```bash
# API 요청
curl http://localhost:8080/api/v1/community/posts

# Jaeger UI 새로고침
# Service: homesweet-back
# Operation: GET 선택
# Find Traces
```
→ Trace가 보이면 OK!

---

## 🎨 추천 쿼리

### Loki
```logql
# ERROR 로그만
{job="homesweet-backend", level="ERROR"}

# 커뮤니티 API 로그
{job="homesweet-backend"} |~ "/api/v1/community.*"

# SQL 쿼리 로그
{job="homesweet-backend"} |= "Hibernate:"
```

### Jaeger
- **느린 요청**: Min Duration > 1s
- **에러 요청**: Tags → `error=true`
- **특정 API**: Operation 필터

---

## 🛠 문제 해결

### Loki에 로그가 안 보임
```bash
# 1. 로그 파일 확인
ls -la logs/application.log

# 2. Promtail 재시작
docker-compose -f docker-compose.monitoring.yml restart promtail

# 3. 앱 재시작
./gradlew bootRun
```

### Jaeger에 Trace가 안 보임
```bash
# API 요청 다시 보내기
curl http://localhost:8080/api/v1/community/posts

# Jaeger UI 새로고침 (F5)
```

---

## 📚 더 알아보기

상세 가이드: [OBSERVABILITY_GUIDE.md](./OBSERVABILITY_GUIDE.md)

---

**5분 완료! 🚀**
