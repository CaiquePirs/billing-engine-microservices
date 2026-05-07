-- Migration for Address and Customers tables
CREATE TABLE address(
    id UUID PRIMARY KEY,
    street VARCHAR(255) NOT NULL,
    number VARCHAR(50) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    county VARCHAR(100) NOT NULL,
    eircode VARCHAR(20) NOT NULL
);

CREATE TYPE customer_status_enum AS ENUM ('ACTIVE', 'INACTIVE', 'DELETED');

CREATE TABLE customers (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(30) NOT NULL,
    tax_number VARCHAR(50) NOT NULL,
    age_int INTEGER NOT NULL,
    date_of_birth DATE NOT NULL,
    address_id UUID REFERENCES address(id),
    customer_status customer_status_enum NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);