-- V2__otp_hash_and_order_unique.sql
-- 1. Expand OTP columns to store SHA-256 64-character hex strings
ALTER TABLE user_email_otp ALTER COLUMN email_otp TYPE VARCHAR(64);
ALTER TABLE forgot_password ALTER COLUMN otp TYPE VARCHAR(64);

-- 2. Add unique constraint to orders.order_number safely
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_orders_order_number'
    ) THEN
        ALTER TABLE orders ADD CONSTRAINT uq_orders_order_number UNIQUE (order_number);
    END IF;
END $$;
