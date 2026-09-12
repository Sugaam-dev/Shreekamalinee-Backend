-- Standardize existing payment method strings in orders and transactions tables
UPDATE orders SET payment_method = 'UPI' WHERE payment_method IN ('WHATSAPP_UPI', 'UPI_DIRECT', 'DIRECT_UPI');
UPDATE orders SET payment_method = 'DIRECT_BANK' WHERE payment_method IN ('BANK_TRANSFER');

UPDATE transactions SET payment_method = 'UPI' WHERE payment_method IN ('WHATSAPP_UPI', 'UPI_DIRECT', 'DIRECT_UPI');
UPDATE transactions SET payment_method = 'DIRECT_BANK' WHERE payment_method IN ('BANK_TRANSFER');
