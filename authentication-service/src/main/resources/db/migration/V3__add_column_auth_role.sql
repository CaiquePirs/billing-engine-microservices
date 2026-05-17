ALTER TABLE authentication ADD COLUMN auth_role VARCHAR(20) NOT NULL;

ALTER TABLE authentication
    ADD CONSTRAINT chk_auth_role CHECK (auth_role IN ('CUSTOMER','TENANT'));