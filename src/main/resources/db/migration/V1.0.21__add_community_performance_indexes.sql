-- 커뮤니티 성능 최적화를 위한 추가 인덱스

-- 1. 게시글 목록 조회 최적화 (삭제되지 않은 게시글, 최신순)
CREATE INDEX idx_community_post_deleted_created 
ON community_post(is_deleted, created_at DESC);

-- 2. 인기 게시글 조회 최적화 (삭제되지 않은 게시글, 조회수순)
CREATE INDEX idx_community_post_deleted_views 
ON community_post(is_deleted, view_count DESC);

-- 3. 댓글 조회 최적화 (특정 게시글의 삭제되지 않은 댓글)
CREATE INDEX idx_community_comment_post_deleted 
ON community_comment(post_id, is_deleted, created_at DESC);

-- 4. 좋아요 조회 최적화
CREATE INDEX idx_community_post_like_post_user 
ON community_post_like(post_id, user_id);

CREATE INDEX idx_community_comment_like_comment_user 
ON community_comment_like(comment_id, user_id);
