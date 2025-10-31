-- ====================================
-- User 엔티티에 맞게 users 테이블 업데이트
-- ====================================

-- 1. users 테이블 구조 변경
ALTER TABLE `users` 
    -- 컬럼명 변경 (user_id는 그대로 유지)
    CHANGE COLUMN `birth` `birth_date` DATE NULL,
    
    -- 기존 컬럼 수정
    MODIFY COLUMN `name` VARCHAR(20) NOT NULL,
    MODIFY COLUMN `email` VARCHAR(100) NOT NULL,
    MODIFY COLUMN `address` VARCHAR(100) NULL,
    MODIFY COLUMN `role` VARCHAR(20) NOT NULL,
    
    -- 새로운 컬럼 추가
    ADD COLUMN `provider` VARCHAR(20) NOT NULL AFTER `address`,
    ADD COLUMN `provider_id` VARCHAR(255) NULL AFTER `provider`,
    ADD COLUMN `phone_number` VARCHAR(20) NULL AFTER `role`;

-- 2. 새로운 유니크 제약조건 추가
ALTER TABLE `users` 
    ADD CONSTRAINT `uk_user_email` UNIQUE (`email`),
    ADD CONSTRAINT `uk_user_provider_id` UNIQUE (`provider`, `provider_id`);

-- 3. Grade 테이블에 기본 데이터 추가
INSERT INTO `grade` (`grade`, `fee_rate`) VALUES 
    ('BRONZE', 0.05),
    ('SILVER', 0.10),
    ('GOLD', 0.15),
    ('VVIP', 0.20),
    ('VIP', 0.25);
