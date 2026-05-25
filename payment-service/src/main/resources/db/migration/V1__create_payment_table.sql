CREATE TABLE payments (
                          id                      UUID            NOT NULL DEFAULT gen_random_uuid(),
                          subscription_id         UUID            NOT NULL,
                          customer_id             UUID            NOT NULL,
                          stripe_payment_intent_id VARCHAR(255)   NOT NULL,
                          stripe_event_id         VARCHAR(255)    NOT NULL UNIQUE,
                          amount                  BIGINT          NOT NULL,
                          currency                VARCHAR(3)      NOT NULL,
                          payment_status          VARCHAR(20)     NOT NULL,
                          raw_payload             JSONB           NOT NULL,
                          processed_at            TIMESTAMP,
                          created_at              TIMESTAMP       NOT NULL DEFAULT now(),
                          updated_at              TIMESTAMP,
                          deleted_at              TIMESTAMP,

                          CONSTRAINT pk_payments PRIMARY KEY (id),
                          CONSTRAINT chk_payment_status CHECK (payment_status IN ('PENDING', 'APPROVED', 'FAILED'))
);

CREATE INDEX idx_payments_subscription_id ON payments (subscription_id);
CREATE INDEX idx_payments_customer_id ON payments (customer_id);
CREATE INDEX idx_payments_stripe_event_id ON payments (stripe_event_id);
CREATE INDEX idx_payments_payment_status ON payments (payment_status);