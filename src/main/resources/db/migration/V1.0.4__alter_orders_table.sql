-- ====================================
-- V1.0.4: orders 및 관련 테이블 구조 변경 (문법 수정판)
-- ====================================
-- 작성일: 2025-10-28
-- 설명: MySQL 8.0 호환성 - IF EXISTS 구문 제거

-- ====================================
-- 1. orders.order_id를 참조하는 모든 외래키 삭제
-- ====================================

-- order_items 외래키 삭제
ALTER TABLE `order_items`
DROP FOREIGN KEY `order_items_ibfk_1`;

ALTER TABLE `order_items`
DROP FOREIGN KEY `order_items_ibfk_2`;

-- payments 외래키 삭제
ALTER TABLE `payments`
DROP FOREIGN KEY `payments_ibfk_1`;

-- settlement 외래키 삭제
ALTER TABLE `settlement`
DROP FOREIGN KEY `settlement_ibfk_1`;

-- ====================================
-- 2. orders 테이블 수정
-- ====================================

-- order_id를 INT에서 BIGINT로 변경
ALTER TABLE `orders`
    MODIFY COLUMN `order_id` BIGINT NOT NULL AUTO_INCREMENT;

-- order_status 길이 확장 (15 -> 20)
ALTER TABLE `orders`
    MODIFY COLUMN `order_status` VARCHAR(20) NOT NULL;

-- delivery_status 컬럼 추가
ALTER TABLE `orders`
    ADD COLUMN `delivery_status` VARCHAR(20) NOT NULL DEFAULT 'BEFORE_SHIPMENT' AFTER `order_status`;

-- updated_at 컬럼 추가
ALTER TABLE `orders`
    ADD COLUMN `updated_at` DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP AFTER `ordered_at`;

-- used_point 컬럼 삭제 (IF EXISTS 제거)
ALTER TABLE `orders`
DROP COLUMN `used_point`;

-- ====================================
-- 3. order_items 테이블 수정
-- ====================================

-- order_id를 INT에서 BIGINT로 변경
ALTER TABLE `order_items`
    MODIFY COLUMN `order_id` BIGINT NOT NULL;

-- product_id를 sku_id로 변경
ALTER TABLE `order_items`
    CHANGE COLUMN `product_id` `sku_id` BIGINT NOT NULL;

-- order_item_status 컬럼 삭제
ALTER TABLE `order_items`
DROP COLUMN `order_item_status`;

-- order_item_price를 price로 변경
ALTER TABLE `order_items`
    CHANGE COLUMN `order_item_price` `price` BIGINT NOT NULL;

-- quantity 타입 변경 (INT -> BIGINT)
ALTER TABLE `order_items`
    MODIFY COLUMN `quantity` BIGINT NOT NULL;

-- ====================================
-- 4. payments 테이블 수정
-- ====================================

-- payment_id를 INT에서 BIGINT로 변경
ALTER TABLE `payments`
    MODIFY COLUMN `payment_id` BIGINT NOT NULL AUTO_INCREMENT;

-- order_id를 INT에서 BIGINT로 변경
ALTER TABLE `payments`
    MODIFY COLUMN `order_id` BIGINT NOT NULL;

-- pg_transaction_id UNIQUE 제약조건 제거
ALTER TABLE `payments`
DROP INDEX `pg_transaction_id`;

-- pg_transaction_id 길이 확장 (50 -> 255)
ALTER TABLE `payments`
    MODIFY COLUMN `pg_transaction_id` VARCHAR(255) NOT NULL;

-- amount 타입 변경 (INT -> BIGINT)
ALTER TABLE `payments`
    MODIFY COLUMN `amount` BIGINT NOT NULL;

-- paid_at NULL 허용
ALTER TABLE `payments`
    MODIFY COLUMN `paid_at` DATETIME NULL;

-- pg_raw_data 컬럼 추가 (JSON)
ALTER TABLE `payments`
    ADD COLUMN `pg_raw_data` JSON NULL;

-- ====================================
-- 5. settlement 테이블 수정
-- ====================================

-- order_id를 INT에서 BIGINT로 변경
ALTER TABLE `settlement`
    MODIFY COLUMN `order_id` BIGINT NOT NULL;

-- ====================================
-- 6. 외래키 제약조건 재생성
-- ====================================

-- order_items 외래키
ALTER TABLE `order_items`
    ADD CONSTRAINT `fk_order_items_order`
        FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`)
            ON DELETE CASCADE;

ALTER TABLE `order_items`
    ADD CONSTRAINT `fk_order_items_sku`
        FOREIGN KEY (`sku_id`) REFERENCES `sku` (`sku_id`)
            ON DELETE RESTRICT;

-- payments 외래키
ALTER TABLE `payments`
    ADD CONSTRAINT `fk_payments_order`
        FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`)
            ON DELETE CASCADE;

-- settlement 외래키
ALTER TABLE `settlement`
    ADD CONSTRAINT `fk_settlement_order`
        FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`)
            ON DELETE CASCADE;

-- ====================================
-- 7. 기존 데이터 처리
-- ====================================

-- 기존 주문 데이터에 delivery_status 설정
UPDATE `orders`
SET `delivery_status` = CASE
                            WHEN `order_status` = 'COMPLETED' THEN 'READY'
                            WHEN `order_status` = 'FAILED' THEN 'CANCELLED'
                            ELSE 'BEFORE_SHIPMENT'
    END
WHERE `delivery_status` = 'BEFORE_SHIPMENT';

-- ====================================
-- 8. 인덱스 추가 (성능 최적화)
-- ====================================

-- order_items 테이블에 sku_id 인덱스 추가
CREATE INDEX `idx_order_items_sku_id` ON `order_items` (`sku_id`);

-- orders 테이블에 user_id, order_status 복합 인덱스 추가
CREATE INDEX `idx_orders_user_status` ON `orders` (`user_id`, `order_status`);

-- orders 테이블에 ordered_at 인덱스 추가
CREATE INDEX `idx_orders_ordered_at` ON `orders` (`ordered_at`);

-- payments 테이블에 order_id 인덱스 추가
CREATE INDEX `idx_payments_order_id` ON `payments` (`order_id`);

-- settlement 테이블에 order_id 인덱스 추가
CREATE INDEX `idx_settlement_order_id` ON `settlement` (`order_id`);

-- ====================================
-- 9. 테이블 코멘트 추가
-- ====================================

ALTER TABLE `orders`
    COMMENT = '주문 테이블 - 사용자의 주문 정보 및 결제/배송 상태 관리';

ALTER TABLE `order_items`
    COMMENT = '주문 상품 테이블 - 주문별 SKU 정보 및 수량, 가격 관리';

ALTER TABLE `payments`
    COMMENT = '결제 테이블 - 주문별 결제 정보 관리 (PG 연동)';

ALTER TABLE `settlement`
    COMMENT = '정산 테이블 - 주문별 판매자 정산 정보 관리';