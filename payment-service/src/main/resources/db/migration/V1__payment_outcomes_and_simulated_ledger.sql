-- Each order is one durable payment operation. Successful outcomes and the
-- simulator's ledger debit are committed together in this local database only.
create table payments (
    order_id uuid primary key,
    payment_id uuid not null unique,
    customer_id varchar(255) not null,
    amount numeric(19,2) not null check (amount > 0),
    status varchar(16) not null check (status in ('SUCCEEDED', 'DECLINED', 'PENDING')),
    reason varchar(255),
    request_fingerprint char(64) not null,
    created_at timestamptz not null default now()
);

create table simulated_payment_ledger (
    order_id uuid primary key,
    payment_id uuid not null unique,
    customer_id varchar(255) not null,
    amount numeric(19,2) not null check (amount > 0),
    created_at timestamptz not null default now(),
    foreign key (order_id) references payments(order_id) deferrable initially deferred
);
