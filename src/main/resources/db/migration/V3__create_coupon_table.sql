-- V3: Fix coupon.discount_type column type from INTEGER to VARCHAR
-- V1 baseline creates coupon with discount_type INTEGER. Exposed expects VARCHAR (enumerationByName).
-- On fresh installs V1 now creates VARCHAR, so this is a no-op.
-- On existing DBs with INTEGER values, the USING clause provides a default mapping.

ALTER TABLE coupon ALTER COLUMN discount_type TYPE VARCHAR(20)
    USING CASE discount_type
        WHEN 0 THEN 'FIXED'
        WHEN 1 THEN 'PERCENTAGE'
        ELSE 'FIXED'
    END;

ALTER TABLE coupon ALTER COLUMN discount_type SET NOT NULL;
