-- V3__data_normalization_and_category_constraints.sql
-- 1. Idempotently remove legacy duplicate UNIQUE constraints on categories.slug
--    Slug uniqueness is properly enforced per parent category via application & indexes
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN (
        SELECT constraint_name 
        FROM information_schema.table_constraints 
        WHERE table_name = 'categories' 
          AND constraint_type = 'UNIQUE' 
          AND constraint_name NOT IN ('uq_category_slug_parent', 'categories_pkey')
    ) LOOP
        EXECUTE 'ALTER TABLE categories DROP CONSTRAINT IF EXISTS ' || quote_ident(r.constraint_name);
    END LOOP;
END $$;

-- 2. Normalize casing on existing production rows for seamless Enum mapping
UPDATE orders SET status = UPPER(TRIM(status)) WHERE status IS NOT NULL;
UPDATE orders SET payment_status = UPPER(TRIM(payment_status)) WHERE payment_status IS NOT NULL;
UPDATE orders SET payment_method = UPPER(TRIM(payment_method)) WHERE payment_method IS NOT NULL;

UPDATE transactions SET status = UPPER(TRIM(status)) WHERE status IS NOT NULL;
UPDATE transactions SET payment_method = UPPER(TRIM(payment_method)) WHERE payment_method IS NOT NULL;

UPDATE coupons SET discount_type = UPPER(TRIM(discount_type)) WHERE discount_type IS NOT NULL;
UPDATE products SET gender_category = UPPER(TRIM(gender_category)) WHERE gender_category IS NOT NULL;
UPDATE contact_messages SET status = UPPER(TRIM(status)) WHERE status IS NOT NULL;