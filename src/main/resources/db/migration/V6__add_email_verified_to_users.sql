-- V6__add_email_verified_to_users.sql
-- Add is_email_verified column to users table if not exists
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_email_verified BOOLEAN DEFAULT FALSE;

-- Existing enabled/active users are marked as verified
UPDATE users SET is_email_verified = TRUE WHERE is_enabled = TRUE;
