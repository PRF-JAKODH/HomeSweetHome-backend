-- ====================================
-- community_images 테이블 image_url 컬럼 길이 확장
-- ====================================

ALTER TABLE community_images
    MODIFY COLUMN image_url VARCHAR(1024) NOT NULL;

-- ====================================
-- community 테이블명 수정
-- ====================================
ALTER TABLE comments RENAME TO community_comments;
ALTER TABLE posts_likes RENAME TO community_post_likes;
ALTER TABLE comments_likes RENAME TO community_comment_likes;

-- ====================================
-- community_posts 테이블에 category 컬럼 추가
-- ====================================
ALTER TABLE community_posts
    ADD COLUMN category VARCHAR(10) NOT NULL DEFAULT 'GENERAL' AFTER content;

