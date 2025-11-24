-- =====================================================
-- V1.0.15: 커뮤니티 게시글 카운트 테이블 분리
-- 목적: 게시글 본문과 카운트(조회수, 좋아요, 댓글수) 락 격리
-- 데드락 방지: S-LOCK → X-LOCK 업그레이드 데드락 근본 해결
-- =====================================================

-- 1. community_post_counts 테이블 생성
CREATE TABLE IF NOT EXISTS community_post_counts (
    post_id BIGINT PRIMARY KEY COMMENT '게시글 ID (PK, FK)',
    view_count INT NOT NULL DEFAULT 0 COMMENT '조회수',
    like_count INT NOT NULL DEFAULT 0 COMMENT '좋아요 수',
    comment_count INT NOT NULL DEFAULT 0 COMMENT '댓글 수',

    -- FK 제약 조건: 게시글 삭제 시 카운트도 함께 삭제
    CONSTRAINT fk_post_count_post_id
        FOREIGN KEY (post_id)
        REFERENCES community_posts(post_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='커뮤니티 게시글 카운트 테이블 (락 분리용)';

-- 2. 기존 community_posts 데이터를 community_post_counts로 마이그레이션
INSERT INTO community_post_counts (post_id, view_count, like_count, comment_count)
SELECT
    post_id,
    COALESCE(view_count, 0) as view_count,
    COALESCE(like_count, 0) as like_count,
    COALESCE(comment_count, 0) as comment_count
FROM community_posts
WHERE post_id NOT IN (SELECT post_id FROM community_post_counts);

-- 3. 인덱스 확인 (PK가 이미 인덱스이므로 추가 불필요)
-- post_id가 PRIMARY KEY이므로 자동으로 클러스터드 인덱스 생성됨

-- =====================================================
-- 참고 사항:
-- 1. community_posts 테이블의 view_count, like_count, comment_count 컬럼은 유지됩니다 (하위 호환성)
-- 2. 새로운 카운트 업데이트는 모두 community_post_counts 테이블을 사용합니다
-- 3. Native SQL UPDATE로 S-LOCK 없이 직접 X-LOCK 획득하여 데드락 방지
-- 4. 게시글 조회 시 필요하면 JOIN으로 카운트 조회 가능
-- =====================================================
