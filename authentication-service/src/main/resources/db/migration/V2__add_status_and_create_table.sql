-- V2: Add AuthStatus to authentication and create internal_authentication with AuthStatus and AuthScope
-- Adds enum-like constraints and defaults compatible with the Java enums AuthStatus and AuthScope

-- Ensure pgcrypto is available for gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Add status (AuthStatus) to existing authentication table
ALTER TABLE authentication
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE authentication
    ADD CONSTRAINT chk_auth_status CHECK (status IN ('ACTIVE','INACTIVE','DELETED'));

-- Create table for InternalAuthentication entity
CREATE TABLE IF NOT EXISTS internal_authentication (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id VARCHAR(255) NOT NULL UNIQUE,
    client_secret_hash VARCHAR(255) NOT NULL,
    scope VARCHAR(50) NOT NULL DEFAULT 'INTERNAL_SERVICE',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT chk_internal_scope CHECK (scope IN ('INTERNAL_SERVICE')),
    CONSTRAINT chk_internal_status CHECK (status IN ('ACTIVE','INACTIVE','DELETED'))
);
