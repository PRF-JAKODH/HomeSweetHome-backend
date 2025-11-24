-- 1. 카테고리별 상품 조회 최적화 (created_at 정렬)
CREATE INDEX idx_product_category_status_created
    ON products (category_id, status, created_at DESC);

-- 2. 카테고리별 상품 조회 최적화 (price 정렬)
CREATE INDEX idx_product_category_status_price
    ON products (category_id, status, base_price DESC);

-- 3. 판매자의 상품 관리 페이지 최적화
CREATE INDEX idx_product_seller_created
    ON products (user_id, created_at DESC);

CREATE INDEX idx_products_status_created_at
    ON products (status, created_at DESC);