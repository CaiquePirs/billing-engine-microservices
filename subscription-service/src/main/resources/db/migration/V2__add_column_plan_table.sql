ALTER TABLE plans
    ADD COLUMN created_by UUID NOT NULL DEFAULT gen_random_uuid();

ALTER TABLE plans
    ALTER COLUMN created_by DROP DEFAULT;

COMMENT ON COLUMN plans.created_by IS 'UUID of the ADMIN who created the plan';