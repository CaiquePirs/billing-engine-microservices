-- Update old values before changing the constraint
UPDATE subscriptions
SET subscription_status = 'CANCELED'
WHERE subscription_status = 'CANCELLED';

-- Remove old constraint
ALTER TABLE subscriptions
DROP CONSTRAINT IF EXISTS chk_subscription_status;

-- Create new constraint with correct values
ALTER TABLE subscriptions
    ADD CONSTRAINT chk_subscription_status
        CHECK (
            subscription_status IN (
                                    'PENDING',
                                    'ACTIVE',
                                    'CANCELED',
                                    'PAST_DUE',
                                    'TRIALING',
                                    'INCOMPLETE'
                )
            );