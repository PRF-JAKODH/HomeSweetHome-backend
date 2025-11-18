ALTER TABLE products
    ADD FULLTEXT INDEX ft_idx_product_search (name, brand, description)
        WITH PARSER ngram;