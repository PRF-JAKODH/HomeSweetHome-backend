ALTER TABLE orders
    ADD COLUMN order_number VARCHAR(36) NULL COMMENT '고유 주문번호 (ORD-UUID)';

UPDATE orders
SET order_number = CONCAT('ORD-', UPPER(REPLACE(UUID(), '-', '')))
WHERE order_number IS NULL;

ALTER TABLE orders
    MODIFY COLUMN order_number VARCHAR(36) NOT NULL;

ALTER TABLE orders
    ADD CONSTRAINT UK_order_number UNIQUE (order_number);