-- month, year 컬럼명 수정(기존 컬럼은 예약어로 취급)
ALTER TABLE weekly_settlements
    RENAME COLUMN month TO month_value;

ALTER TABLE weekly_settlements
    RENAME COLUMN year TO year_value;

ALTER TABLE monthly_settlements
    RENAME COLUMN month TO month_value;

ALTER TABLE monthly_settlements
    RENAME COLUMN year TO year_value;

ALTER TABLE yearly_settlements
    RENAME COLUMN year TO year_value;