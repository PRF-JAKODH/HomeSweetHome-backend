-- Product Review Statistics Table
-- 통계 기반 조회 성능 최적화를 위해 생성
CREATE TABLE product_review_statistics (
                                           product_id       BIGINT NOT NULL PRIMARY KEY,
                                           review_count     INT NOT NULL DEFAULT 0,
                                           average_rating   DECIMAL(3,2) NOT NULL DEFAULT 0.00,
                                           last_review_at   DATETIME NULL,
                                           updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                               ON UPDATE CURRENT_TIMESTAMP,

                                           CONSTRAINT fk_product_review_statistics_product
                                               FOREIGN KEY (product_id)
                                                   REFERENCES products(product_id)
                                                   ON DELETE CASCADE
);

-- 조회 최적화를 위한 인덱스
CREATE INDEX idx_prs_review_count ON product_review_statistics (review_count DESC);
CREATE INDEX idx_prs_average_rating ON product_review_statistics (average_rating DESC);
CREATE INDEX idx_prs_last_review_at ON product_review_statistics (last_review_at DESC);