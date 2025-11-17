CREATE INDEX idx_products_status_created_at
    ON products (status, created_at DESC);