-- V6: Remove stock_quantity from product table; inventory table is the single source of truth
-- This migration handles existing databases that have the column.

ALTER TABLE product DROP COLUMN IF EXISTS stock_quantity;
