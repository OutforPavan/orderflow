CREATE TABLE checkout_orders (
    id UUID PRIMARY KEY,
    customer_id VARCHAR(200) NOT NULL,
    idempotency_key VARCHAR(120) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    product_id BIGINT NOT NULL CHECK (product_id > 0),
    quantity INTEGER NOT NULL CHECK (quantity BETWEEN 1 AND 1000),
    service_level VARCHAR(30) NOT NULL,
    payment_method_reference VARCHAR(100) NOT NULL,
    state VARCHAR(30) NOT NULL CHECK (state IN ('RESERVE_PENDING','PAYMENT_PENDING','RELEASE_PENDING','CONFIRMED','REJECTED','CANCELLED','REVIEW_REQUIRED')),
    unit_price NUMERIC(19,2),
    subtotal NUMERIC(19,2),
    discount_percent NUMERIC(5,2),
    discount_amount NUMERIC(19,2),
    priority_surcharge_percent NUMERIC(5,2),
    priority_surcharge_amount NUMERIC(19,2),
    total NUMERIC(19,2),
    reason VARCHAR(200),
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    lease_owner UUID,
    lease_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    UNIQUE(customer_id, idempotency_key)
);
CREATE INDEX checkout_due ON checkout_orders(next_attempt_at)
    WHERE state IN ('RESERVE_PENDING','PAYMENT_PENDING','RELEASE_PENDING');
