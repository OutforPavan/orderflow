-- Preserve existing orders as standard, undiscounted price snapshots.
ALTER TABLE purchase_orders
    ADD COLUMN subtotal NUMERIC(22, 2),
    ADD COLUMN discount_percent NUMERIC(5, 2) NOT NULL DEFAULT 0,
    ADD COLUMN discount_amount NUMERIC(22, 2) NOT NULL DEFAULT 0,
    ADD COLUMN priority_surcharge_percent NUMERIC(5, 2) NOT NULL DEFAULT 0,
    ADD COLUMN priority_surcharge_amount NUMERIC(22, 2) NOT NULL DEFAULT 0,
    ADD COLUMN service_level VARCHAR(20) NOT NULL DEFAULT 'STANDARD';

UPDATE purchase_orders SET subtotal = unit_price * quantity;
ALTER TABLE purchase_orders ALTER COLUMN subtotal SET NOT NULL;

-- V1 did not explicitly name its total constraint. Find that one constraint
-- through its referenced columns, without assuming PostgreSQL's generated name.
DO $$
DECLARE old_constraint TEXT;
BEGIN
    SELECT c.conname INTO STRICT old_constraint
    FROM pg_constraint c
    JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY(c.conkey)
    WHERE c.conrelid = 'purchase_orders'::regclass
      AND c.contype = 'c' AND a.attname = 'total';
    EXECUTE format('ALTER TABLE purchase_orders DROP CONSTRAINT %I', old_constraint);
END;
$$;

ALTER TABLE purchase_orders
    ADD CONSTRAINT order_subtotal_matches CHECK (subtotal = unit_price * quantity),
    ADD CONSTRAINT order_discount_valid CHECK (discount_percent BETWEEN 0 AND 100
        AND discount_amount = round(subtotal * discount_percent / 100, 2)),
    ADD CONSTRAINT order_priority_valid CHECK (
        (service_level = 'STANDARD' AND priority_surcharge_percent = 0) OR
        (service_level = 'PRIORITY_5' AND priority_surcharge_percent = 5) OR
        (service_level = 'PRIORITY_10' AND priority_surcharge_percent = 10)),
    ADD CONSTRAINT order_priority_amount_matches CHECK (
        priority_surcharge_amount = round((subtotal - discount_amount) * priority_surcharge_percent / 100, 2)),
    ADD CONSTRAINT order_total_matches CHECK (
        total = subtotal - discount_amount + priority_surcharge_amount AND total >= 0);
