ALTER TABLE payments
    ALTER COLUMN stripe_payment_intent_id DROP NOT NULL;

ALTER TABLE payments
    ALTER COLUMN stripe_event_id DROP NOT NULL;

ALTER TABLE payments
    ALTER COLUMN raw_payload DROP NOT NULL;