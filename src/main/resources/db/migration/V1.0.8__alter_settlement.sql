ALTER TABLE weekly_settlements
MODIFY COLUMN year SMALLINT NOT NULL;

ALTER TABLE monthly_settlements
MODIFY COLUMN year SMALLINT NOT NULL;

ALTER TABLE yearly_settlements
    MODIFY COLUMN year SMALLINT NOT NULL;

ALTER TABLE settlement
DROP COLUMN order_canceled;

ALTER TABLE weekly_settlements
    ADD COLUMN total_vat DECIMAL(15,2);

ALTER TABLE monthly_settlements
    ADD COLUMN total_vat DECIMAL(15,2);

ALTER TABLE yearly_settlements
    ADD COLUMN total_vat DECIMAL(15,2);
