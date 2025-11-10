-- ===========================
-- 1 카테고리
-- ===========================
INSERT INTO product_category (category_id, name, parent_id, depth)
VALUES (1, '가구', NULL, 0),
       (2, '의자', 1, 1),
       (3, '책상', 1, 1);

-- ===========================
-- 2 유저 (판매자)
-- ===========================
INSERT INTO users (user_id, name, email, provider, role)
VALUES (10, '판매자A', 'sellerA@google.com', 'google', 'USER'),
       (11, '판매자B', 'sellerB@google.com', 'google', 'USER'),
       (12, '판매자C', 'sellerC@google.com', 'google', 'USER');

-- ===========================
-- 3 상품
-- ===========================
INSERT INTO products (
    product_id, category_id, user_id, name, image_url,
    brand, base_price, discount_rate, description,
    shipping_price, status, created_at, updated_at
) VALUES
      -- ① 오래된 상품 (30일 전)
      (100, 2, 10, '고급 의자', 'https://a.jpg', '홈스윗', 10000, 10.00, '좋은 의자', 3000, 'ON_SALE',
       DATEADD('DAY', -30, NOW()), DATEADD('DAY', -30, NOW())),

      -- ② 10일 전 등록된 상품
      (101, 2, 10, '저가 의자', 'https://b.jpg', '가구나라', 5000, 0.00, '싼 의자', 3000, 'ON_SALE',
       DATEADD('DAY', -10, NOW()), DATEADD('DAY', -10, NOW())),

      -- ③ 최근(어제) 등록된 상품
      (102, 3, 11, '책상 세트', 'https://c.jpg', '홈스윗', 30000, 5.00, '좋은 책상', 3000, 'ON_SALE',
       DATEADD('DAY', -1, NOW()), DATEADD('DAY', -1, NOW())),

      -- ④ 오늘 등록된 판매 중지 상품
      (103, 3, 11, '판매 중지 상품', 'https://d.jpg', '가구나라', 15000, 0.00, '품절 상품', 3000, 'SUSPENDED',
       NOW(), NOW());

-- ===========================
-- 4 상품 상세 이미지
-- ===========================
INSERT INTO products_detail_image (id, product_id, image_url, created_at)
VALUES
    (1000, 100, 'https://a_detail_1.jpg', NOW()),
    (1001, 100, 'https://a_detail_2.jpg', NOW()),
    (1002, 102, 'https://c_detail_1.jpg', NOW());

-- ===========================
-- 5 리뷰
-- ===========================
INSERT INTO products_reviews (review_id, product_id, user_id, rating, comment, created_at, updated_at)
VALUES
    (1, 100, 10, 5, '좋아요!', NOW(), NOW()),
    (2, 100, 11, 4, '괜찮아요', NOW(), NOW()),
    (3, 102, 12, 3, '보통이에요', NOW(), NOW());

-- ===========================
-- 6 옵션 그룹
-- ===========================
INSERT INTO product_option_group (option_group_id, product_id, group_name, created_at)
VALUES (200, 100, '색상', NOW()),
       (201, 100, '사이즈', NOW());

-- ===========================
-- 7 옵션 값
-- ===========================
INSERT INTO product_option_value (option_value_id, option_group_id, value_name, created_at)
VALUES (300, 200, '화이트', NOW()),
       (301, 200, '블랙', NOW()),
       (302, 201, 'S', NOW()),
       (303, 201, 'L', NOW());

-- ===========================
-- 8 SKU (상품별 조합)
-- ===========================
INSERT INTO sku (sku_id, product_id, stock_quantity, price_adjustment, created_at, updated_at)
VALUES
    (400, 100, 10, 0, NOW(), NOW()),     -- 화이트 + S
    (401, 100, 5, 5000, NOW(), NOW());   -- 블랙 + L

-- ===========================
-- 9 SKU 옵션 매핑
-- ===========================
INSERT INTO product_sku_option (sku_option_id, sku_id, option_value_id, created_at)
VALUES
    (500, 400, 300, NOW()),  -- 화이트
    (501, 400, 302, NOW()),  -- S
    (502, 401, 301, NOW()),  -- 블랙
    (503, 401, 303, NOW());  -- L

-- ===========================
-- 10 장바구니
-- ===========================
INSERT INTO carts (cart_id, user_id, sku_id, quantity, created_at, updated_at)
VALUES
    (600, 10, 400, 1, NOW(), NOW()),   -- 판매자A, 화이트 S 의자
    (601, 10, 401, 2, NOW(), NOW()),   -- 판매자A, 블랙 L 의자
    (602, 11, 400, 3, NOW(), NOW());   -- 판매자B, 화이트 S 의자