-- Standardize address_type column in shipping_addresses to uppercase
UPDATE shipping_addresses 
SET address_type = CASE 
    WHEN UPPER(TRIM(address_type)) = 'HOME' THEN 'HOME'
    WHEN UPPER(TRIM(address_type)) = 'WORK' THEN 'WORK'
    WHEN UPPER(TRIM(address_type)) = 'OTHER' THEN 'OTHER'
    WHEN UPPER(TRIM(address_type)) = 'MANUAL_ORDER' THEN 'MANUAL_ORDER'
    ELSE 'HOME'
END
WHERE address_type IS NOT NULL;
