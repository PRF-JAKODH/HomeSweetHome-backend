-- year 타입 변경
ALTER TABLE weekly_settlements
MODIFY COLUMN year SMALLINT NOT NULL;

ALTER TABLE monthly_settlements
MODIFY COLUMN year SMALLINT NOT NULL;

ALTER TABLE yearly_settlements
    MODIFY COLUMN year SMALLINT NOT NULL;

-- 주문취소여부 컬럼 삭제
ALTER TABLE settlement
DROP COLUMN order_canceled;

-- 부과세 컬럼 추가
ALTER TABLE weekly_settlements
    ADD COLUMN total_vat DECIMAL(15,2);

ALTER TABLE monthly_settlements
    ADD COLUMN total_vat DECIMAL(15,2);

ALTER TABLE yearly_settlements
    ADD COLUMN total_vat DECIMAL(15,2);

-- 사용하지 않는 컬럼 삭제
ALTER TABLE weekly_settlements
    DROP COLUMN daily_sales;

ALTER TABLE weekly_settlements
    DROP COLUMN weekly_sales;

-- upsert시 값이 중복되지 않게 하기 위해 unique 설정
CREATE UNIQUE INDEX ux_daily_settlements
ON daily_settlements(user_id, settlement_date);

CREATE UNIQUE INDEX ux_weekly_settlements
ON weekly_settlements(user_id, week_start_date);

CREATE UNIQUE INDEX ux_monthly_settlements
ON monthly_settlements(user_id, year, month);

CREATE UNIQUE INDEX ux_yearly_settlements
ON yearly_settlements(user_id, year);

-- ALTER TABLE order_items
--     ADD COLUMN seller_id BIGINT NOT NULL;