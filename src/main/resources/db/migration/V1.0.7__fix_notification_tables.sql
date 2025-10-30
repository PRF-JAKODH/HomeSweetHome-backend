-- ====================================
-- 알림 시스템 테이블 수정
-- ====================================

-- 1. notification_category에 created_at 컬럼 추가
ALTER TABLE `notification_category` 
    ADD COLUMN `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER `category_name`;

-- 2. notification 테이블을 notification_template로 이름 변경
ALTER TABLE `notification` RENAME TO `notification_template`;

-- 3. notification_template 테이블 구조 수정
ALTER TABLE `notification_template`
    CHANGE COLUMN `notification_id` `notification_template_id` BIGINT NOT NULL AUTO_INCREMENT,
    ADD COLUMN `template_type` VARCHAR(50) NOT NULL AFTER `notification_category_id`,
    ADD COLUMN `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `created_at`;

-- 3-1. notification_template 컬럼 길이 수정 (title, content)
ALTER TABLE `notification_template`
    MODIFY COLUMN `title` VARCHAR(50) NOT NULL,
    MODIFY COLUMN `content` VARCHAR(200) NOT NULL;

-- 4. user_notification 테이블 수정
-- 외래키 제약조건 이름을 찾아서 제거 (동적 SQL)
SET @fk_name = NULL;

SELECT CONSTRAINT_NAME INTO @fk_name
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'user_notification'
  AND REFERENCED_TABLE_NAME = 'notification'
  LIMIT 1;

SET @sql = IF(@fk_name IS NOT NULL, 
    CONCAT('ALTER TABLE user_notification DROP FOREIGN KEY `', @fk_name, '`'), 
    'SELECT 1 as noop');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 컬럼명 변경
ALTER TABLE `user_notification`
    CHANGE COLUMN `notification_id` `notification_template_id` BIGINT DEFAULT NULL;

-- 새로운 외래키 추가
ALTER TABLE `user_notification`
    ADD CONSTRAINT `fk_user_notification_template` 
        FOREIGN KEY (`notification_template_id`) REFERENCES `notification_template` (`notification_template_id`) ON DELETE SET NULL;

-- 5. notification_template의 외래키 제약조건 재생성
-- 외래키 제약조건 이름을 찾아서 제거
SET @fk_name = NULL;

SELECT CONSTRAINT_NAME INTO @fk_name
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'notification_template'
  AND REFERENCED_TABLE_NAME = 'notification_category'
  LIMIT 1;

SET @sql = IF(@fk_name IS NOT NULL,
    CONCAT('ALTER TABLE notification_template DROP FOREIGN KEY `', @fk_name, '`'),
    'SELECT 1 as noop');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 새로운 외래키 추가
ALTER TABLE `notification_template`
    ADD CONSTRAINT `fk_notification_template_category` 
        FOREIGN KEY (`notification_category_id`) REFERENCES `notification_category` (`notification_category_id`) ON DELETE RESTRICT;



-- ====================================
-- 알림 시스템 초기 데이터
-- ====================================

-- 알림 카테고리 데이터 삽입
INSERT INTO notification_category (category_name) VALUES 
    ('ORDER'),
    ('PAYMENT'),
    ('COMMUNITY'),
    ('SETTLEMENT'),
    ('PRODUCT'),
    ('CHAT'),
    ('SYSTEM'),
    ('PROMOTION'),
    ('CUSTOM')
ON DUPLICATE KEY UPDATE category_name = VALUES(category_name);

-- 알림 템플릿 데이터 삽입
INSERT INTO notification_template (notification_category_id, template_type, title, content, redirect_url) VALUES 
    -- 주문 관련
    (1, 'ORDER_COMPLETED', '주문 완료', '{userName}님의 주문이 완료되었습니다. (주문번호: {orderId})', '/orders/{orderId}'),
    (1, 'ORDER_CANCELLED', '주문 취소', '{userName}님의 주문이 취소되었습니다. (주문번호: {orderId})', '/orders/{orderId}'),
    (1, 'ORDER_SHIPPED', '배송 시작', '{userName}님의 주문이 배송을 시작했습니다. (주문번호: {orderId})', '/orders/{orderId}'),
    (1, 'ORDER_DELIVERED', '배송 완료', '{userName}님의 주문이 배송 완료되었습니다. (주문번호: {orderId})', '/orders/{orderId}'),
    
    -- 결제 관련
    (2, 'PAYMENT_SUCCESS', '결제 성공', '{userName}님의 결제가 성공적으로 완료되었습니다. (금액: {amount}원)', '/payments/{paymentId}'),
    (2, 'PAYMENT_FAILED', '결제 실패', '{userName}님의 결제가 실패했습니다. (주문번호: {orderId})', '/orders/{orderId}'),
    (2, 'PAYMENT_REFUNDED', '환불 완료', '{userName}님의 환불이 완료되었습니다. (금액: {amount}원)', '/payments/{paymentId}'),
    
    -- 커뮤니티 관련
    (3, 'NEW_COMMENT', '새 댓글', '{userName}님이 {postTitle}에 댓글을 남겼습니다.', '/community/posts/{postId}'),
    (3, 'NEW_LIKE', '새 좋아요', '{userName}님이 {postTitle}에 좋아요를 눌렀습니다.', '/community/posts/{postId}'),
    
    -- 정산 관련
    (4, 'SETTLEMENT_COMPLETED', '정산 완료', '{userName}님의 {settlementName} 정산이 완료되었습니다. (금액: {amount}원)', '/settlements/{settlementId}'),
    
    -- 상품 관련
    (5, 'PRODUCT_APPROVED', '상품 승인', '{userName}님의 상품이 승인되었습니다. (상품명: {productName})', '/products/{productId}'),
    (5, 'PRODUCT_REJECTED', '상품 거부', '{userName}님의 상품이 거부되었습니다. (상품명: {productName})', '/products/{productId}'),
    (5, 'PRODUCT_LOW_STOCK', '재고 부족', '{userName}님의 {productName} 상품 재고가 부족합니다. (현재 재고: {currentStock})', '/products/{productId}'),
    
    -- 채팅 관련
    (6, 'NEW_MESSAGE', '새 메시지', '{userName}님이 {roomName} 채팅방에서 메시지를 보냈습니다: {message}', '/chat/{roomId}'),
    
    -- 시스템 관련
    (7, 'SYSTEM_MAINTENANCE', '시스템 점검', '시스템 점검 안내: {maintenanceTime}', '/system/notice'),
    (7, 'SYSTEM_UPDATE', '시스템 업데이트', '시스템이 업데이트되었습니다. (버전: {version})', '/system/update'),
    
    -- 프로모션 관련
    (8, 'PROMOTION_START', '프로모션 시작', '{promotionName} 프로모션이 시작되었습니다!', '/promotions/{promotionId}'),
    (8, 'PROMOTION_END', '프로모션 종료', '{promotionName} 프로모션이 종료되었습니다.', '/promotions/{promotionId}')
ON DUPLICATE KEY UPDATE 
    title = VALUES(title), 
    content = VALUES(content), 
    redirect_url = VALUES(redirect_url);
