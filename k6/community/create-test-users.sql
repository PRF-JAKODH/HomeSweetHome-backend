-- ====================================================================
-- Multi-User Test Data Generator for K6 Tests
-- ====================================================================
-- 
-- 🎯 목적: k6 테스트에서 여러 유저가 다른 ID로 좋아요를 누를 수 있도록
--         테스트용 유저를 미리 생성
--
-- 📝 실행 방법:
--   docker exec -i homesweet-db mysql -u user -ppassword homesweet < k6/community/create-test-users.sql
-- or:
--   mysql -h localhost -P 3306 -u user -ppassword homesweet < k6/community/create-test-users.sql
--
-- ⚠️ 주의: 프로덕션 환경에서는 실행하지 마세요!
-- ====================================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- Disable foreign key checks for faster insertion
SET FOREIGN_KEY_CHECKS = 0;

-- Set common timestamp
SET @now = NOW();

-- ====================================================================
-- 1. Create Test Users (2 ~ 500)
-- ====================================================================
-- k6 테스트에서 testUserId 파라미터로 사용될 유저들

DELIMITER $$

DROP PROCEDURE IF EXISTS generate_test_users$$
CREATE PROCEDURE generate_test_users()
BEGIN
    DECLARE i INT DEFAULT 2;
    DECLARE email_val VARCHAR(100);
    DECLARE name_val VARCHAR(50);
    
    WHILE i <= 500 DO
        SET email_val = CONCAT('test.user', i, '@k6.com');
        SET name_val = CONCAT('K6 Test User ', i);
        
        -- Insert if not exists
        INSERT IGNORE INTO users (user_id, email, name, profile_img_url, provider, role, created_at)
        VALUES
            (i, email_val, name_val, 'https://example.com/avatar.jpg', 'GOOGLE', 'USER', @now);
        
        SET i = i + 1;
    END WHILE;
END$$

DELIMITER ;

CALL generate_test_users();
DROP PROCEDURE IF EXISTS generate_test_users;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- ====================================================================
-- 2. Verification
-- ====================================================================
SELECT 'Test Users Generation Complete' AS status;
SELECT '' AS separator;

SELECT 
    'Total Test Users' AS metric,
    COUNT(*) AS count 
FROM users 
WHERE user_id BETWEEN 2 AND 500;

SELECT '' AS separator;
SELECT 'Sample Test Users (first 10)' AS info;

SELECT 
    user_id,
    email,
    name,
    provider,
    DATE_FORMAT(created_at, '%Y-%m-%d') as created_date
FROM users
WHERE user_id BETWEEN 2 AND 20
ORDER BY user_id
LIMIT 10;
