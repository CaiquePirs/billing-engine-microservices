CREATE TABLE plans (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500) NOT NULL,
    price NUMERIC(19,2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    interval VARCHAR(20) NOT NULL,
    stripe_price_id VARCHAR(255) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL
);

CREATE TABLE subscriptions (
    id UUID PRIMARY KEY,
    plan_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    stripe_subscription_id VARCHAR(255) NOT NULL UNIQUE,
    current_period_start DATE NOT NULL,
    current_period_end DATE NOT NULL,
    subscription_status VARCHAR(30) NOT NULL,

    CONSTRAINT fk_subscription_plan
        FOREIGN KEY (plan_id)
        REFERENCES plans(id)
        ON DELETE RESTRICT
);

-- CHECK CONSTRAINTS
ALTER TABLE plans
ADD CONSTRAINT chk_plan_interval
CHECK (interval IN ('MONTHLY', 'YEARLY'));

ALTER TABLE subscriptions
ADD CONSTRAINT chk_subscription_status
CHECK (
    subscription_status IN (
        'ACTIVE',
        'CANCELLED',
        'PAST_DUE',
        'TRIALING'
    )
);