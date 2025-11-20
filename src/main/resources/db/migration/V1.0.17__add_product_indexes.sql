-- 1. 카테고리별 상품 조회 최적화 (created_at 정렬)
CREATE INDEX idx_product_category_status_created
    ON products (category_id, status, created_at DESC);