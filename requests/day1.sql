-- Read-only observations for the database selected in your SQL client.
SELECT current_database();

-- Flyway's version history: application startup applies V1 once per database.
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;

-- Compare these rows before and after the HTTP requests or a service transaction.
SELECT id, name, price, stock FROM products ORDER BY id;
SELECT id, product_id, quantity, unit_price, total, created_at
FROM purchase_orders ORDER BY id;

-- An existing order retains the price used at creation after a product price change.
SELECT o.id AS order_id, o.unit_price AS ordered_price, p.price AS current_price
FROM purchase_orders o JOIN products p ON p.id = o.product_id
ORDER BY o.id;
