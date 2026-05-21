ALTER TABLE customers
    ADD COLUMN stripe_customer_id VARCHAR UNIQUE NOT NULL;