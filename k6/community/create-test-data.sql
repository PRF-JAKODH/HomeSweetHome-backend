-- 테스트 데이터 생성 SQL
--
-- 실행 방법:
-- docker exec -i homesweet-db mysql -u user -ppassword homesweet < k6-tests/create-test-data.sql
--
-- 또는:
-- mysql -h localhost -P 3306 -u user -ppassword homesweet < k6-tests/create-test-data.sql

-- 1. 테스트용 사용자 생성 (OAuth2 사용자가 없을 경우)
INSERT IGNORE INTO users (user_id, email, name, profile_img_url, provider, role)
VALUES
  (9999, 'test.user@k6.com', 'K6 Test User', 'https://example.com/avatar.jpg', 'GOOGLE', 'USER');

-- 2. 커뮤니티 게시글 생성 (10개)
INSERT IGNORE INTO community_posts (post_id, user_id, title, content, category)
VALUES
  (1, 9999, '부하 테스트용 게시글 1', '이것은 k6 부하 테스트를 위한 샘플 게시글입니다.', 'GENERAL'),
  (2, 9999, '부하 테스트용 게시글 2', 'N+1 문제 확인을 위한 게시글입니다.', 'QUESTION'),
  (3, 9999, '부하 테스트용 게시글 3', '조회수 동시성 테스트용 게시글입니다.', 'INFO'),
  (4, 9999, '부하 테스트용 게시글 4', '스파이크 테스트 데이터입니다.', 'GENERAL'),
  (5, 9999, '부하 테스트용 게시글 5', '스트레스 테스트 데이터입니다.', 'QUESTION'),
  (6, 9999, '부하 테스트용 게시글 6', '성능 모니터링 테스트용입니다.', 'INFO'),
  (7, 9999, '부하 테스트용 게시글 7', 'Prometheus 메트릭 수집 테스트입니다.', 'GENERAL'),
  (8, 9999, '부하 테스트용 게시글 8', 'Jaeger 트레이싱 테스트입니다.', 'QUESTION'),
  (9, 9999, '부하 테스트용 게시글 9', 'Grafana 대시보드 확인용입니다.', 'INFO'),
  (10, 9999, '부하 테스트용 게시글 10', '종합 부하 테스트용 게시글입니다.', 'GENERAL');

-- 3. 커뮤니티 이미지 (각 게시글마다 2개씩)
INSERT IGNORE INTO community_images (image_id, post_id, image_url, image_order)
VALUES
  (1, 1, 'https://example.com/image1-1.jpg', 1),
  (2, 1, 'https://example.com/image1-2.jpg', 2),
  (3, 2, 'https://example.com/image2-1.jpg', 1),
  (4, 2, 'https://example.com/image2-2.jpg', 2),
  (5, 3, 'https://example.com/image3-1.jpg', 1),
  (6, 3, 'https://example.com/image3-2.jpg', 2),
  (7, 4, 'https://example.com/image4-1.jpg', 1),
  (8, 4, 'https://example.com/image4-2.jpg', 2),
  (9, 5, 'https://example.com/image5-1.jpg', 1),
  (10, 5, 'https://example.com/image5-2.jpg', 2);

-- 4. 댓글 생성 (각 게시글마다 3개씩)
INSERT IGNORE INTO community_comments (comment_id, post_id, user_id, content, parent_comment_id)
VALUES
  (1, 1, 9999, '첫 번째 댓글입니다.', NULL),
  (2, 1, 9999, '두 번째 댓글입니다.', NULL),
  (3, 1, 9999, '대댓글입니다.', 1),
  (4, 2, 9999, '좋은 정보네요!', NULL),
  (5, 2, 9999, '감사합니다.', NULL),
  (6, 2, 9999, '도움이 되었습니다.', NULL),
  (7, 3, 9999, '동시성 테스트 댓글', NULL),
  (8, 3, 9999, 'Lock 테스트', NULL),
  (9, 3, 9999, '비관적 락 확인', NULL);

-- 5. 데이터 확인
SELECT
  '✅ 게시글 생성 완료' as status,
  COUNT(*) as count
FROM community_posts
WHERE is_deleted = 0;

SELECT
  '✅ 이미지 생성 완료' as status,
  COUNT(*) as count
FROM community_images;

SELECT
  '✅ 댓글 생성 완료' as status,
  COUNT(*) as count
FROM community_comments
WHERE is_deleted = 0;

-- 6. 샘플 데이터 조회
SELECT
  post_id,
  title,
  category,
  view_count,
  like_count,
  comment_count
FROM community_posts
WHERE is_deleted = 0
ORDER BY post_id
LIMIT 10;
