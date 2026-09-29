-- R__seed_reference_data: idempotent reference seeds (no secrets, no users).
INSERT INTO shipping_method (id, name, type, price, delivery_time) VALUES
    ('seed-ship-standard', 'Standard', 'FLAT', 5.00, '3-5 business days'),
    ('seed-ship-express', 'Express', 'FLAT', 15.00, '1-2 business days'),
    ('seed-ship-free', 'Free Shipping', 'FLAT', 0.00, '5-7 business days')
ON CONFLICT (id) DO NOTHING;

INSERT INTO category (id, name) VALUES
    ('seed-cat-electronics', 'Electronics'),
    ('seed-cat-fashion', 'Fashion'),
    ('seed-cat-home', 'Home & Living'),
    ('seed-cat-grocery', 'Grocery')
ON CONFLICT (id) DO NOTHING;
