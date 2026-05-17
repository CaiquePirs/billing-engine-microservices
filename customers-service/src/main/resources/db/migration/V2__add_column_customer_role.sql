ALTER TABLE customers
    ADD COLUMN customer_role VARCHAR(20) NOT NULL;

ALTER TABLE customers
    ADD CONSTRAINT chk_customer_role
        CHECK (customer_role IN ('CUSTOMER', 'TENANT'));