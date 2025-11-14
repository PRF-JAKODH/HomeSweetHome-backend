# 🗄️ k6 테스트 데이터 설정 가이드

k6 부하 테스트를 실행하기 전에 테스트 데이터를 생성하는 방법입니다.

## ⚡ 빠른 설정 (30초)

### 1단계: SQL 실행
```bash
# Docker 컨테이너를 통해 MySQL에 테스트 데이터 삽입
docker exec -i homesweet-db mysql -u user -ppassword homesweet < k6-tests/create-test-data.sql
```

### 2단계: 데이터 확인
```bash
curl http://localhost:8080/api/v1/community/posts | jq '.totalElements'
# 출력: 게시글 수
```

### 3단계: k6 테스트 실행
```bash
k6 run --vus 10 --duration 10s k6-tests/community-load-test.js
```

---

## 📊 생성되는 테스트 데이터

### 사용자
- **ID:** 9999
- **이메일:** test.user@k6.com
- **이름:** K6 Test User

### 게시글 (10개)
- ID 1~10
- 카테고리: GENERAL, QUESTION, INFO
- 제목: "부하 테스트용 게시글 1~10"

### 이미지 (10개)
- 게시글 1~5번에 각각 2개씩

### 댓글 (9개)
- 게시글 1~3번에 각각 3개씩
- 대댓글 포함

---

## 🔍 데이터 확인 방법

### MySQL 직접 조회
```bash
docker exec -it homesweet-db mysql -u user -ppassword homesweet
```

```sql
-- 게시글 수 확인
SELECT COUNT(*) FROM community_posts WHERE is_deleted = 0;

-- 게시글 목록
SELECT post_id, title, category, view_count
FROM community_posts
WHERE is_deleted = 0
ORDER BY post_id
LIMIT 10;

-- 댓글 수 확인
SELECT COUNT(*) FROM community_comments WHERE is_deleted = 0;
```

### API로 확인
```bash
# 게시글 목록
curl http://localhost:8080/api/v1/community/posts | jq

# 특정 게시글 조회
curl http://localhost:8080/api/v1/community/posts/1 | jq

# 댓글 조회
curl http://localhost:8080/api/v1/community/posts/1/comments | jq
```

### k6 스크립트로 확인
```bash
k6 run k6-tests/setup-test-data.js
```

---

## 🗑️ 테스트 데이터 삭제

### 전체 삭제
```sql
DELETE FROM community_comments WHERE user_id = 9999;
DELETE FROM community_images WHERE post_id IN (1,2,3,4,5,6,7,8,9,10);
DELETE FROM community_posts WHERE user_id = 9999;
DELETE FROM users WHERE user_id = 9999;
```

### SQL 파일로 삭제
```bash
docker exec -i homesweet-db mysql -u user -ppassword homesweet << 'EOF'
DELETE FROM community_comments WHERE user_id = 9999;
DELETE FROM community_images WHERE post_id IN (1,2,3,4,5,6,7,8,9,10);
DELETE FROM community_posts WHERE user_id = 9999;
DELETE FROM users WHERE user_id = 9999;
EOF
```

---

## 🔄 데이터 재생성

테스트 데이터가 망가졌거나 초기화가 필요한 경우:

```bash
# 1. 기존 데이터 삭제
docker exec -i homesweet-db mysql -u user -ppassword homesweet << 'EOF'
DELETE FROM community_comments WHERE user_id = 9999;
DELETE FROM community_images WHERE post_id IN (1,2,3,4,5,6,7,8,9,10);
DELETE FROM community_posts WHERE user_id = 9999;
DELETE FROM users WHERE user_id = 9999;
EOF

# 2. 새 데이터 생성
docker exec -i homesweet-db mysql -u user -ppassword homesweet < k6-tests/create-test-data.sql

# 3. 확인
curl http://localhost:8080/api/v1/community/posts/1 | jq '.postId'
```

---

## ⚠️ 주의사항

### 1. AUTO_INCREMENT 문제
SQL에서 `INSERT IGNORE`를 사용하므로 중복 실행시:
- 기존 데이터는 유지됨
- 새 데이터는 추가되지 않음

### 2. 외래키 제약
삭제시 순서 중요:
1. 댓글 (community_comments)
2. 이미지 (community_images)
3. 게시글 (community_posts)
4. 사용자 (users)

### 3. 운영 환경 주의
- 이 스크립트는 개발/테스트 환경 전용입니다
- 운영 DB에서 절대 실행하지 마세요!

---

## 🎯 다음 단계

데이터 생성 후:

```bash
# 1. 부하 테스트 실행
k6 run --out experimental-prometheus-rw k6-tests/community-load-test.js

# 2. Grafana에서 실시간 모니터링
# http://localhost:3001

# 3. Jaeger에서 트레이싱 확인
# http://localhost:16686
```

---

**Happy Testing! 🚀**
