-- V3: Fix coupon.discount_type column type from INTEGER to VARCHAR
-- V1 baseline creates coupon with discount_type INTEGER. Exposed expects VARCHAR (enumerationByName).
-- The conversion runs ONLY when the column is still INTEGER (upgraded DBs).
-- On fresh installs V1 already creates VARCHAR, so the block is a safe no-op
-- (a bare CASE ... WHEN 0 would crash on VARCHAR with "operator does not exist").
DO $$ BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'coupon' AND column_name = 'discount_type' AND data_type = 'integer'
    ) THEN
        ALTER TABLE coupon ALTER COLUMN discount_type TYPE VARCHAR(20)
            USING CASE discount_type
                WHEN 0 THEN 'FIXED'
                WHEN 1 THEN 'PERCENTAGE'
                ELSE 'FIXED'
            END;
    END IF;
END $$;

ALTER TABLE coupon ALTER COLUMN discount_type SET NOT NULL;
