ALTER TABLE weekly_settlements
MODIFY COLUMN year SMALLINT NOT NULL;

ALTER TABLE monthly_settlements
MODIFY COLUMN year SMALLINT NOT NULL;

ALTER TABLE yearly_settlements
    MODIFY COLUMN year SMALLINT NOT NULL;

ALTER TABLE settlement
DROP COLUMN order_canceled;
