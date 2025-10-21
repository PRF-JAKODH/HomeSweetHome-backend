-- ====================================
-- H2용 스키마 파일 (V1.0.1 + V1.0.2 통합)
-- ====================================

-- Grade 테이블
CREATE TABLE grade (
    grade_id INT NOT NULL AUTO_INCREMENT,
    grade VARCHAR(10) NULL,
    fee_rate DECIMAL(5,2) NULL,
    PRIMARY KEY (grade_id)
);

-- Users 테이블 (V1.0.1 + V1.0.2 통합)
CREATE TABLE users (
    user_id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL,
    address VARCHAR(100) NULL,
    birth_date DATE NULL,
    profile_img_url VARCHAR(255) NULL,
    role VARCHAR(20) NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_id VARCHAR(255) NULL,
    phone_number VARCHAR(20) NULL,
    grade_id INT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    CONSTRAINT uk_user_email UNIQUE (email),
    CONSTRAINT uk_user_provider_id UNIQUE (provider, provider_id),
    FOREIGN KEY (grade_id) REFERENCES grade (grade_id)
);
