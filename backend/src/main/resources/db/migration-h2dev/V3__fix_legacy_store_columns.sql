-- Same retrofit as V2, for `stores` rows that predate `category`/`total_price` becoming
-- required columns. No-op on a DB whose `stores` table came fresh from V1.
ALTER TABLE stores ADD COLUMN IF NOT EXISTS category ENUM('CLOTHES','LIGHT','MEDIUM','OTHER');
UPDATE stores SET category = 'OTHER' WHERE category IS NULL;
ALTER TABLE stores ALTER COLUMN category SET NOT NULL;

ALTER TABLE stores ADD COLUMN IF NOT EXISTS total_price INT;
UPDATE stores SET total_price = 0 WHERE total_price IS NULL;
ALTER TABLE stores ALTER COLUMN total_price SET NOT NULL;
