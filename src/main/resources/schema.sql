CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY,
    status VARCHAR(16) NOT NULL,
    card_number VARCHAR(512) NOT NULL,
    expiry_month INTEGER NOT NULL CHECK (expiry_month BETWEEN 1 AND 12),
    expiry_year INTEGER NOT NULL,
    currency VARCHAR(3) NOT NULL,
    amount NUMERIC(38, 0) NOT NULL,
    cvv VARCHAR(512) NOT NULL,
    authorization_code UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_payments_authorization_code
    ON payments (authorization_code);

CREATE INDEX IF NOT EXISTS ix_payments_status_created_at
    ON payments (status, created_at);
