-- A shop must never have two concurrently open cash drawers.
-- Application code checks first, but this index closes the race.
CREATE UNIQUE INDEX IF NOT EXISTS pos_session_one_open_per_shop
    ON pos_session(shop_id)
    WHERE status = 'OPEN';
