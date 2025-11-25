-- ====================================================================
-- Community Performance Test Data Generator
-- ====================================================================
-- Execution:
--   docker exec -i homesweet-db mysql -u user -ppassword homesweet < k6/community/create-test-data.sql
-- or:
--   mysql -h localhost -P 3306 -u user -ppassword homesweet < k6/community/create-test-data.sql
-- ====================================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- Disable foreign key checks for faster insertion
SET FOREIGN_KEY_CHECKS = 0;

-- Set common timestamp
SET @now = NOW();

-- ====================================================================
-- 1. Create Test User
-- ====================================================================
INSERT IGNORE INTO users (user_id, email, name, profile_img_url, provider, role, created_at)
VALUES
    (9999, 'test.user@k6.com', 'K6 Test User', 'https://example.com/avatar.jpg', 'GOOGLE', 'USER', @now);

-- ====================================================================
-- 2. Create Posts (1000 posts)
-- ====================================================================

DELIMITER $$

DROP PROCEDURE IF EXISTS generate_posts$$
CREATE PROCEDURE generate_posts()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE category_val VARCHAR(50);
    DECLARE title_val VARCHAR(200);
    DECLARE content_val VARCHAR(1000);

    WHILE i <= 1000 DO
        -- Random category selection
        SET category_val = ELT(1 + FLOOR(RAND() * 5), 'Question', 'Info', 'Review', 'Discussion', 'Notice');

        -- Generate title and content
        SET title_val = CONCAT('Performance Test Post ', i);
        SET content_val = CONCAT(
            'This is a test post for load testing. Post number: ', i, '. ',
            'Testing N+1 query issues, concurrency control, and database performance. ',
            'Category: ', category_val
        );

        INSERT IGNORE INTO community_posts
            (post_id, user_id, title, content, category, view_count, like_count, comment_count, is_modified, created_at, is_deleted)
        VALUES
            (i, 9999, title_val, content_val, category_val, 0, 0, 0, FALSE, DATE_SUB(@now, INTERVAL FLOOR(RAND() * 30) DAY), FALSE);

        SET i = i + 1;
    END WHILE;
END$$

DELIMITER ;

CALL generate_posts();
DROP PROCEDURE IF EXISTS generate_posts;

-- ====================================================================
-- 3. Create Comments (3000-5000 comments, average 3-5 per post)
-- ====================================================================

DELIMITER $$

DROP PROCEDURE IF EXISTS generate_comments$$
CREATE PROCEDURE generate_comments()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE comment_id_counter INT DEFAULT 1;
    DECLARE num_comments INT;
    DECLARE j INT;
    DECLARE content_val VARCHAR(500);
    DECLARE parent_id INT;

    WHILE i <= 1000 DO
        -- Random number of comments per post (0-10, weighted towards 3-5)
        SET num_comments = FLOOR(RAND() * 11);

        -- Only create comments for 80% of posts
        IF RAND() < 0.8 THEN
            SET j = 1;
            WHILE j <= num_comments DO
                SET content_val = CONCAT('Test comment ', comment_id_counter, ' for post ', i);

                -- 10% chance to be a reply (parent_comment_id not NULL)
                SET parent_id = NULL;
                IF j > 1 AND RAND() < 0.1 THEN
                    SET parent_id = comment_id_counter - FLOOR(RAND() * (j - 1)) - 1;
                END IF;

                INSERT IGNORE INTO community_comments
                    (comment_id, post_id, user_id, content, parent_comment_id, like_count, is_modified, created_at, is_deleted)
                VALUES
                    (comment_id_counter, i, 9999, content_val, parent_id, 0, FALSE, DATE_SUB(@now, INTERVAL FLOOR(RAND() * 30) DAY), FALSE);

                SET comment_id_counter = comment_id_counter + 1;
                SET j = j + 1;
            END WHILE;

            -- Update post comment count
            UPDATE community_posts
            SET comment_count = num_comments
            WHERE post_id = i;
        END IF;

        SET i = i + 1;
    END WHILE;
END$$

DELIMITER ;

CALL generate_comments();
DROP PROCEDURE IF EXISTS generate_comments;

-- ====================================================================
-- 4. Create Images (30% of posts have 1-3 images)
-- ====================================================================

DELIMITER $$

DROP PROCEDURE IF EXISTS generate_images$$
CREATE PROCEDURE generate_images()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE image_id_counter INT DEFAULT 1;
    DECLARE num_images INT;
    DECLARE j INT;
    DECLARE image_url_val VARCHAR(500);

    WHILE i <= 1000 DO
        -- 30% of posts have images
        IF RAND() < 0.3 THEN
            SET num_images = 1 + FLOOR(RAND() * 3); -- 1-3 images
            SET j = 1;

            WHILE j <= num_images DO
                SET image_url_val = CONCAT('https://picsum.photos/800/600?random=', image_id_counter);

                INSERT IGNORE INTO community_images
                    (image_id, post_id, image_url, image_order, created_at)
                VALUES
                    (image_id_counter, i, image_url_val, j, DATE_SUB(@now, INTERVAL FLOOR(RAND() * 30) DAY));

                SET image_id_counter = image_id_counter + 1;
                SET j = j + 1;
            END WHILE;
        END IF;

        SET i = i + 1;
    END WHILE;
END$$

DELIMITER ;

CALL generate_images();
DROP PROCEDURE IF EXISTS generate_images;

-- ====================================================================
-- 5. Create Post Likes (random distribution)
-- ====================================================================

DELIMITER $$

DROP PROCEDURE IF EXISTS generate_post_likes$$
CREATE PROCEDURE generate_post_likes()
BEGIN
    DECLARE i INT DEFAULT 1;

    WHILE i <= 1000 DO
        -- 20% of posts get a like from test user
        IF RAND() < 0.2 THEN
            INSERT IGNORE INTO community_post_likes
                (post_id, user_id, created_at)
            VALUES
                (i, 9999, DATE_SUB(@now, INTERVAL FLOOR(RAND() * 30) DAY));

            UPDATE community_posts
            SET like_count = like_count + 1
            WHERE post_id = i;
        END IF;

        SET i = i + 1;
    END WHILE;
END$$

DELIMITER ;

CALL generate_post_likes();
DROP PROCEDURE IF EXISTS generate_post_likes;

-- ====================================================================
-- 6. Create Comment Likes (random distribution)
-- ====================================================================

DELIMITER $$

DROP PROCEDURE IF EXISTS generate_comment_likes$$
CREATE PROCEDURE generate_comment_likes()
BEGIN
    DECLARE max_comment_id INT;
    DECLARE i INT DEFAULT 1;

    SELECT IFNULL(MAX(comment_id), 0) INTO max_comment_id FROM community_comments;

    WHILE i <= max_comment_id DO
        -- 15% of comments get a like from test user
        IF RAND() < 0.15 THEN
            INSERT IGNORE INTO community_comment_likes
                (comment_id, user_id, created_at)
            VALUES
                (i, 9999, DATE_SUB(@now, INTERVAL FLOOR(RAND() * 30) DAY));

            UPDATE community_comments
            SET like_count = like_count + 1
            WHERE comment_id = i;
        END IF;

        SET i = i + 1;
    END WHILE;
END$$

DELIMITER ;

CALL generate_comment_likes();
DROP PROCEDURE IF EXISTS generate_comment_likes;

-- ====================================================================
-- 7. Create Hot Posts (for concurrency testing)
-- ====================================================================

-- Make posts 1-10 popular (high view counts and likes)
UPDATE community_posts
SET
    view_count = 500 + FLOOR(RAND() * 500),
    like_count = like_count + 50 + FLOOR(RAND() * 100)
WHERE post_id BETWEEN 1 AND 10;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- ====================================================================
-- 8. Verification Queries
-- ====================================================================

SELECT 'Test Data Generation Complete' AS status;
SELECT '' AS separator;

SELECT 'Users' AS table_name, COUNT(*) AS count FROM users WHERE user_id = 9999
UNION ALL
SELECT 'Posts', COUNT(*) FROM community_posts WHERE is_deleted = FALSE
UNION ALL
SELECT 'Comments', COUNT(*) FROM community_comments WHERE is_deleted = FALSE
UNION ALL
SELECT 'Images', COUNT(*) FROM community_images
UNION ALL
SELECT 'Post Likes', COUNT(*) FROM community_post_likes
UNION ALL
SELECT 'Comment Likes', COUNT(*) FROM community_comment_likes;

SELECT '' AS separator;
SELECT 'Top 10 Posts by Views' AS info;

SELECT
    post_id,
    title,
    category,
    view_count,
    like_count,
    comment_count,
    DATE_FORMAT(created_at, '%Y-%m-%d') as created_date
FROM community_posts
WHERE is_deleted = FALSE
ORDER BY view_count DESC
LIMIT 10;

SELECT '' AS separator;
SELECT 'Sample Comments' AS info;

SELECT
    comment_id,
    post_id,
    LEFT(content, 50) as content_preview,
    parent_comment_id,
    like_count,
    DATE_FORMAT(created_at, '%Y-%m-%d') as created_date
FROM community_comments
WHERE is_deleted = FALSE
ORDER BY comment_id
LIMIT 10;

SELECT '' AS separator;
SELECT 'Posts with Images' AS info;

SELECT
    p.post_id,
    p.title,
    COUNT(i.image_id) as image_count
FROM community_posts p
JOIN community_images i ON p.post_id = i.post_id
GROUP BY p.post_id, p.title
ORDER BY image_count DESC
LIMIT 10;
