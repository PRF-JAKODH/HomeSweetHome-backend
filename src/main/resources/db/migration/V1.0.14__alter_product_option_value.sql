ALTER TABLE product_option_value
    RENAME COLUMN value TO value_name;

ALTER TABLE products
    MODIFY COLUMN name VARCHAR(100) NOT NULL,
    MODIFY COLUMN brand VARCHAR(100) NOT NULL;