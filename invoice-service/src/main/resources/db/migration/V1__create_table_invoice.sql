CREATE TABLE invoices (
    id UUID PRIMARY KEY,
    subscription_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    payment_id UUID,
    amount BIGINT NOT NULL,
    currency VARCHAR(10) NOT NULL,
    invoice_status VARCHAR(20) NOT NULL,
    s3_key VARCHAR(255),
    due_date DATE NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

