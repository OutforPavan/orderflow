CREATE TABLE products (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(200) NOT NULL CHECK (length(trim(name)) > 0),
    price NUMERIC(12, 2) NOT NULL CHECK (price > 0),
    stock INTEGER NOT NULL CHECK (stock >= 0 AND stock <= 1000000000)
);

CREATE TABLE reservations (
    order_id UUID PRIMARY KEY,
    product_id BIGINT,
    quantity INTEGER,
    unit_price NUMERIC(12, 2),
    status VARCHAR(16) NOT NULL CHECK (status IN ('RESERVED', 'REJECTED', 'RELEASED')),
    reason VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (product_id IS NULL OR product_id > 0),
    CHECK (quantity IS NULL OR (quantity > 0 AND quantity <= 1000000)),
    CHECK (unit_price IS NULL OR unit_price > 0),
    CHECK ((product_id IS NULL) = (quantity IS NULL)),
    CHECK (product_id IS NOT NULL OR status = 'RELEASED'),
    CHECK (status <> 'RESERVED' OR (product_id IS NOT NULL AND unit_price IS NOT NULL))
);

-- No product foreign key: a stable PRODUCT_NOT_FOUND rejection still records the requested ID.
-- There is deliberately no TTL cleanup of reservation keys or cancellation tombstones.
