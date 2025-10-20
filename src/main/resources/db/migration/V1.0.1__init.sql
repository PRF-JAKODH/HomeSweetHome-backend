-- ====================================
-- 사용자 및 등급 테이블
-- ====================================

CREATE TABLE `grade` (
                         `grade_id` INT NOT NULL AUTO_INCREMENT,
                         `grade` VARCHAR(10) NOT NULL,
                         `fee_rate` DECIMAL(5,2) NOT NULL,
                         PRIMARY KEY (`grade_id`)
);

CREATE TABLE `users` (
                         `user_id` BIGINT NOT NULL AUTO_INCREMENT,
                         `name` VARCHAR(20) NULL,
                         `email` VARCHAR(100) NOT NULL UNIQUE,
                         `address` VARCHAR(100) NULL,
                         `birth` DATE NULL,
                         `profile_img_url` VARCHAR(255) NULL,
                         `role` VARCHAR(100) NOT NULL,
                         `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         `grade_id` INT NULL,
                         PRIMARY KEY (`user_id`),
                         FOREIGN KEY (`grade_id`) REFERENCES `grade` (`grade_id`)
);

-- ====================================
-- 상품 관련 테이블
-- ====================================

CREATE TABLE `product_category` (
                                    `category_id` BIGINT NOT NULL AUTO_INCREMENT,
                                    `name` VARCHAR(50) NOT NULL,
                                    `parent_id` BIGINT NULL,
                                    `depth` TINYINT NULL,
                                    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                    PRIMARY KEY (`category_id`),
                                    FOREIGN KEY (`parent_id`) REFERENCES `product_category` (`category_id`) ON DELETE SET NULL
);

CREATE TABLE `products` (
                            `product_id` BIGINT NOT NULL AUTO_INCREMENT,
                            `category_id` BIGINT NOT NULL,
                            `user_id` BIGINT NOT NULL,
                            `name` VARCHAR(30) NOT NULL,
                            `image_url` VARCHAR(255) NOT NULL,
                            `brand` VARCHAR(20) NOT NULL,
                            `base_price` INT NOT NULL DEFAULT 0,
                            `discount_rate` DECIMAL(5,2) NOT NULL DEFAULT 0.0,
                            `description` VARCHAR(255) NULL,
                            `shipping_price` INT NOT NULL DEFAULT 0,
                            `status` VARCHAR(12) NOT NULL,
                            `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            PRIMARY KEY (`product_id`),
                            FOREIGN KEY (`category_id`) REFERENCES `product_category` (`category_id`),
                            FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
);

CREATE TABLE `products_detail_image` (
                                         `id` BIGINT NOT NULL AUTO_INCREMENT,
                                         `product_id` BIGINT NOT NULL,
                                         `image_url` VARCHAR(255) NOT NULL,
                                         `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                         PRIMARY KEY (`id`),
                                         FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE CASCADE
);

CREATE TABLE `product_option` (
                                  `option_id` BIGINT NOT NULL AUTO_INCREMENT,
                                  `product_id` BIGINT NOT NULL,
                                  `option_name` VARCHAR(12) NOT NULL,
                                  `value` VARCHAR(12) NOT NULL,
                                  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  PRIMARY KEY (`option_id`),
                                  FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE CASCADE
);

CREATE TABLE `sku` (
                       `sku_id` BIGINT NOT NULL AUTO_INCREMENT,
                       `product_id` BIGINT NOT NULL,
                       `price_adjustment` INT NULL DEFAULT 0,
                       `stock_quantity` INT NULL DEFAULT 0,
                       `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       PRIMARY KEY (`sku_id`),
                       FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE CASCADE
);

CREATE TABLE `product_sku_option` (
                                      `sku_option_id` BIGINT NOT NULL AUTO_INCREMENT,
                                      `sku_id` BIGINT NOT NULL,
                                      `option_id` BIGINT NOT NULL,
                                      `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      PRIMARY KEY (`sku_option_id`),
                                      FOREIGN KEY (`sku_id`) REFERENCES `sku` (`sku_id`) ON DELETE CASCADE,
                                      FOREIGN KEY (`option_id`) REFERENCES `product_option` (`option_id`) ON DELETE CASCADE
);

CREATE TABLE `products_reviews` (
                                    `review_id` BIGINT NOT NULL AUTO_INCREMENT,
                                    `product_id` BIGINT NOT NULL,
                                    `user_id` BIGINT NOT NULL,
                                    `rating` SMALLINT NOT NULL CHECK (`rating` BETWEEN 1 AND 5),
                                    `comment` VARCHAR(150) NULL,
                                    `image_url` VARCHAR(255) NULL,
                                    `like_count` INT NULL DEFAULT 0,
                                    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                    PRIMARY KEY (`review_id`),
                                    FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE CASCADE,
                                    FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
);

-- ====================================
-- 주문 및 결제 테이블
-- ====================================

CREATE TABLE `orders` (
                          `order_id` INT NOT NULL AUTO_INCREMENT,
                          `user_id` BIGINT NOT NULL,
                          `order_status` VARCHAR(15) NOT NULL,
                          `total_amount` BIGINT NOT NULL,
                          `ordered_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          `used_point` INT NOT NULL DEFAULT 0,
                          PRIMARY KEY (`order_id`),
                          FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
);

CREATE TABLE `order_items` (
                               `order_item_id` BIGINT NOT NULL AUTO_INCREMENT,
                               `order_id` INT NOT NULL,
                               `product_id` BIGINT NOT NULL,
                               `quantity` INT NOT NULL,
                               `order_item_status` VARCHAR(15) NOT NULL,
                               `order_item_price` INT NOT NULL,
                               PRIMARY KEY (`order_item_id`),
                               FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`) ON DELETE CASCADE,
                               FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`)
);

CREATE TABLE `payments` (
                            `payment_id` INT NOT NULL AUTO_INCREMENT,
                            `order_id` INT NOT NULL,
                            `pg_transaction_id` VARCHAR(50) NOT NULL UNIQUE,
                            `amount` INT NOT NULL,
                            `method` VARCHAR(20) NOT NULL,
                            `payment_status` VARCHAR(15) NOT NULL,
                            `paid_at` DATETIME NOT NULL,
                            PRIMARY KEY (`payment_id`),
                            FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`) ON DELETE CASCADE
);

-- ====================================
-- 장바구니 테이블
-- ====================================

CREATE TABLE `carts` (
                         `cart_id` BIGINT NOT NULL AUTO_INCREMENT,
                         `user_id` BIGINT NOT NULL,
                         `sku_id` BIGINT NOT NULL,
                         `quantity` INT NOT NULL DEFAULT 1,
                         `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         PRIMARY KEY (`cart_id`),
                         FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
                         FOREIGN KEY (`sku_id`) REFERENCES `sku` (`sku_id`) ON DELETE CASCADE
);

-- ====================================
-- 정산 관련 테이블
-- ====================================

CREATE TABLE `daily_settlements` (
                                     `daily_id` BIGINT NOT NULL AUTO_INCREMENT,
                                     `user_id` BIGINT NOT NULL,
                                     `total_sales` DECIMAL(15,2) NULL DEFAULT 0.00,
                                     `total_fee` DECIMAL(15,2) NULL DEFAULT 0.00,
                                     `total_vat` DECIMAL(15,2) NULL DEFAULT 0.00,
                                     `total_refund` DECIMAL(15,2) NULL DEFAULT 0.00,
                                     `total_settlement` DECIMAL(15,2) NULL DEFAULT 0.00,
                                     `settlement_date` DATETIME NULL,
                                     PRIMARY KEY (`daily_id`),
                                     FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
);

CREATE TABLE `weekly_settlements` (
                                      `weekly_id` BIGINT NOT NULL AUTO_INCREMENT,
                                      `user_id` BIGINT NOT NULL,
                                      `year` TINYINT NOT NULL,
                                      `month` TINYINT NOT NULL,
                                      `week_start_date` DATE NULL,
                                      `week_end_date` DATE NULL,
                                      `daily_sales` DECIMAL(15,2) NULL DEFAULT 0.00,
                                      `weekly_sales` DECIMAL(15,2) NULL DEFAULT 0.00,
                                      `total_sales` DECIMAL(15,2) NULL DEFAULT 0.00,
                                      `total_fee` DECIMAL(15,2) NULL DEFAULT 0.00,
                                      `total_refund` DECIMAL(15,2) NULL DEFAULT 0.00,
                                      `total_settlement` DECIMAL(15,2) NULL DEFAULT 0.00,
                                      PRIMARY KEY (`weekly_id`),
                                      FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
);

CREATE TABLE `monthly_settlements` (
                                       `monthly_id` BIGINT NOT NULL AUTO_INCREMENT,
                                       `user_id` BIGINT NOT NULL,
                                       `year` TINYINT NOT NULL,
                                       `month` TINYINT NOT NULL,
                                       `total_sales` DECIMAL(15,2) NULL DEFAULT 0.00,
                                       `total_fee` DECIMAL(15,2) NULL DEFAULT 0.00,
                                       `total_refund` DECIMAL(15,2) NULL DEFAULT 0.00,
                                       `total_settlement` DECIMAL(15,2) NULL DEFAULT 0.00,
                                       PRIMARY KEY (`monthly_id`),
                                       FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
);

CREATE TABLE `yearly_settlements` (
                                      `yearly_id` BIGINT NOT NULL AUTO_INCREMENT,
                                      `user_id` BIGINT NOT NULL,
                                      `year` TINYINT NOT NULL,
                                      `total_sales` DECIMAL(15,2) NULL DEFAULT 0.00,
                                      `total_fee` DECIMAL(15,2) NULL DEFAULT 0.00,
                                      `total_refund` DECIMAL(15,2) NULL DEFAULT 0.00,
                                      `total_settlement` DECIMAL(15,2) NULL DEFAULT 0.00,
                                      PRIMARY KEY (`yearly_id`),
                                      FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
);

CREATE TABLE `settlement` (
                              `settlement_id` BIGINT NOT NULL AUTO_INCREMENT,
                              `order_id` INT NOT NULL,
                              `user_id` BIGINT NOT NULL,
                              `settlement_status` VARCHAR(10) NULL,
                              `sales_amount` DECIMAL(15,2) NULL DEFAULT 0.00,
                              `fee` DECIMAL(15,2) NULL DEFAULT 0.00,
                              `vat` DECIMAL(15,2) NULL DEFAULT 0.00,
                              `refund_amount` DECIMAL(15,2) NULL DEFAULT 0.00,
                              `settlement_amount` DECIMAL(15,2) NULL DEFAULT 0.00,
                              `settlement_date` DATETIME NULL,
                              `order_canceled` BOOLEAN NULL DEFAULT FALSE,
                              PRIMARY KEY (`settlement_id`),
                              FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`),
                              FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
);

-- ====================================
-- 커뮤니티 관련 테이블
-- ====================================

CREATE TABLE `community_posts` (
                                   `post_id` BIGINT NOT NULL AUTO_INCREMENT,
                                   `user_id` BIGINT NOT NULL,
                                   `title` VARCHAR(100) NOT NULL,
                                   `content` VARCHAR(1000) NOT NULL,
                                   `view_count` INT NOT NULL DEFAULT 0,
                                   `like_count` INT NOT NULL DEFAULT 0,
                                   `comment_count` INT NOT NULL DEFAULT 0,
                                   `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                   `is_modified` BOOLEAN NOT NULL DEFAULT FALSE,
                                   `modified_at` TIMESTAMP NULL,
                                   `is_deleted` BOOLEAN NOT NULL DEFAULT FALSE,
                                   PRIMARY KEY (`post_id`),
                                   FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
);

CREATE TABLE `community_images` (
                                    `image_id` BIGINT NOT NULL AUTO_INCREMENT,
                                    `post_id` BIGINT NOT NULL,
                                    `image_url` VARCHAR(255) NOT NULL,
                                    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    `image_order` INT NOT NULL CHECK (`image_order` BETWEEN 1 AND 5),
                                    PRIMARY KEY (`image_id`),
                                    FOREIGN KEY (`post_id`) REFERENCES `community_posts` (`post_id`) ON DELETE CASCADE
);

CREATE TABLE `posts_likes` (
                               `post_id` BIGINT NOT NULL,
                               `user_id` BIGINT NOT NULL,
                               `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               PRIMARY KEY (`post_id`, `user_id`),
                               FOREIGN KEY (`post_id`) REFERENCES `community_posts` (`post_id`) ON DELETE CASCADE,
                               FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
);

CREATE TABLE `comments` (
                            `comment_id` BIGINT NOT NULL AUTO_INCREMENT,
                            `post_id` BIGINT NOT NULL,
                            `user_id` BIGINT NOT NULL,
                            `parent_comment_id` BIGINT NULL,
                            `content` VARCHAR(500) NOT NULL,
                            `like_count` INT NOT NULL DEFAULT 0,
                            `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            `is_modified` BOOLEAN NOT NULL DEFAULT FALSE,
                            `modified_at` TIMESTAMP NULL,
                            `is_deleted` BOOLEAN NOT NULL DEFAULT FALSE,
                            PRIMARY KEY (`comment_id`),
                            FOREIGN KEY (`post_id`) REFERENCES `community_posts` (`post_id`) ON DELETE CASCADE,
                            FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
                            FOREIGN KEY (`parent_comment_id`) REFERENCES `comments` (`comment_id`) ON DELETE CASCADE
);

CREATE TABLE `comments_likes` (
                                  `comment_id` BIGINT NOT NULL,
                                  `user_id` BIGINT NOT NULL,
                                  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  PRIMARY KEY (`comment_id`, `user_id`),
                                  FOREIGN KEY (`comment_id`) REFERENCES `comments` (`comment_id`) ON DELETE CASCADE,
                                  FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
);

-- ====================================
-- 채팅 관련 테이블
-- ====================================

CREATE TABLE `chat_room` (
                             `room_id` BIGINT NOT NULL AUTO_INCREMENT,
                             `room_type` VARCHAR(20) NOT NULL,
                             `name` VARCHAR(100) NOT NULL,
                             `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
                             `last_message_id` BIGINT NULL,
                             `thumbnail_url` VARCHAR(255) NULL,
                             PRIMARY KEY (`room_id`),
                             CONSTRAINT `chk_chat_room_type` CHECK (`room_type` IN ('INDIVIDUAL','GROUP'))
);

CREATE TABLE `room_member` (
                               `room_member_id` BIGINT NOT NULL AUTO_INCREMENT,
                               `user_id` BIGINT NOT NULL,
                               `room_id` BIGINT NOT NULL,
                               `role` VARCHAR(100) NOT NULL,
                               `is_exit` BOOLEAN NOT NULL DEFAULT FALSE,
                               `last_read_message_id` BIGINT NULL,
                               PRIMARY KEY (`room_member_id`),
                               FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
                               FOREIGN KEY (`room_id`) REFERENCES `chat_room` (`room_id`) ON DELETE CASCADE,
                               UNIQUE KEY `unique_room_member` (`user_id`, `room_id`),
                               CONSTRAINT `chk_room_member_role`
                                   CHECK (`role` IN ('OWNER','MEMBER'))
);

CREATE TABLE `chat_image` (
                              `image_id` BIGINT NOT NULL AUTO_INCREMENT,
                              `file_url` VARCHAR(255) NOT NULL,
                              `thumbnail_url` VARCHAR(255) NOT NULL,
                              `file_size` INT NOT NULL,
                              `uploaded_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              `file_type` VARCHAR(100) NOT NULL CHECK (`file_type` IN ('jpg', 'png', 'gif', 'webp', 'mp4', 'webm')),
                              PRIMARY KEY (`image_id`)
);

CREATE TABLE `chat_message` (
                                `chat_message_id` BIGINT NOT NULL AUTO_INCREMENT,
                                `room_id` BIGINT NOT NULL,
                                `sender_id` BIGINT NOT NULL,
                                `message_type` VARCHAR(100) NOT NULL,
                                `message_content` TEXT NULL,
                                `sent_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                `image_id` BIGINT NULL,
                                PRIMARY KEY (`chat_message_id`),
                                FOREIGN KEY (`room_id`) REFERENCES `chat_room` (`room_id`) ON DELETE CASCADE,
                                FOREIGN KEY (`sender_id`) REFERENCES `users` (`user_id`),
                                FOREIGN KEY (`image_id`) REFERENCES `chat_image` (`image_id`)
);

-- ====================================
-- 알림 관련 테이블
-- ====================================

CREATE TABLE `notification_category` (
                                         `notification_category_id` INT NOT NULL AUTO_INCREMENT,
                                         `category_name` VARCHAR(50) NOT NULL,
                                         PRIMARY KEY (`notification_category_id`)
);

CREATE TABLE `notification` (
                                `notification_id` BIGINT NOT NULL AUTO_INCREMENT,
                                `notification_category_id` INT NOT NULL,
                                `title` VARCHAR(30) NOT NULL,
                                `content` VARCHAR(50) NOT NULL,
                                `redirect_url` VARCHAR(255) NOT NULL,
                                `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                PRIMARY KEY (`notification_id`),
                                FOREIGN KEY (`notification_category_id`) REFERENCES `notification_category` (`notification_category_id`)
);

CREATE TABLE `user_notification` (
                                     `user_notification_id` BIGINT NOT NULL AUTO_INCREMENT,
                                     `user_id` BIGINT NOT NULL,
                                     `notification_id` BIGINT NOT NULL,
                                     `context_data` JSON NOT NULL,
                                     `is_read` BOOLEAN NOT NULL DEFAULT FALSE,
                                     `is_deleted` BOOLEAN NOT NULL DEFAULT FALSE,
                                     `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     PRIMARY KEY (`user_notification_id`),
                                     FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
                                     FOREIGN KEY (`notification_id`) REFERENCES `notification` (`notification_id`) ON DELETE CASCADE
);