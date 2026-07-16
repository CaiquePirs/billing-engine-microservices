-- Remove duplicate PENDING payments created by SQS redeliveries, keeping the oldest per subscription
DELETE FROM payments p
USING payments older
WHERE p.subscription_id = older.subscription_id
  AND p.id <> older.id
  AND p.payment_status = 'PENDING'
  AND p.created_at > older.created_at;

-- One payment per subscription, enforced at the database level
DROP INDEX idx_payments_subscription_id;
CREATE UNIQUE INDEX idx_payments_subscription_id ON payments (subscription_id) WHERE deleted_at IS NULL;
