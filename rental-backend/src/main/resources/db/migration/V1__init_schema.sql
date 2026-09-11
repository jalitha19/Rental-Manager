-- ============================================================
-- V1: Initial schema for the Rental Management System
-- ============================================================

-- ---------- users ----------
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(100) NOT NULL UNIQUE,
    password_hash   TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------- properties ----------
CREATE TABLE properties (
    id              BIGSERIAL PRIMARY KEY,
    property_code   VARCHAR(30) NOT NULL UNIQUE,
    type            VARCHAR(20) NOT NULL CHECK (type IN ('HOUSE', 'ROOM')),
    name            VARCHAR(150) NOT NULL,
    address         TEXT NOT NULL,
    monthly_rent    NUMERIC(12,2) NOT NULL CHECK (monthly_rent >= 0),
    deposit         NUMERIC(12,2) CHECK (deposit >= 0),
    status          VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE'
                        CHECK (status IN ('AVAILABLE', 'OCCUPIED', 'MAINTENANCE')),
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------- tenants ----------
CREATE TABLE tenants (
    id              BIGSERIAL PRIMARY KEY,
    tenant_type     VARCHAR(20) NOT NULL DEFAULT 'INDIVIDUAL'
                        CHECK (tenant_type IN ('INDIVIDUAL', 'COMPANY')),
    full_name       VARCHAR(150) NOT NULL,
    nic_number      VARCHAR(50),
    contact_person  VARCHAR(150),
    address         TEXT,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Fast case-insensitive search on name / NIC
CREATE INDEX idx_tenants_full_name_lower ON tenants (LOWER(full_name));
CREATE INDEX idx_tenants_nic_number ON tenants (nic_number);

-- ---------- tenant_phones ----------
CREATE TABLE tenant_phones (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    phone_number    VARCHAR(30) NOT NULL,
    label           VARCHAR(30)
);

CREATE INDEX idx_tenant_phones_tenant_id ON tenant_phones (tenant_id);
CREATE INDEX idx_tenant_phones_number ON tenant_phones (phone_number);

-- ---------- rentals ----------
CREATE TABLE rentals (
    id                  BIGSERIAL PRIMARY KEY,
    property_id         BIGINT NOT NULL REFERENCES properties(id),
    tenant_id           BIGINT NOT NULL REFERENCES tenants(id),
    start_date          DATE NOT NULL,
    end_date            DATE,
    monthly_rent        NUMERIC(12,2) NOT NULL CHECK (monthly_rent >= 0),
    deposit             NUMERIC(12,2) CHECK (deposit >= 0),
    payment_due_day     INTEGER CHECK (payment_due_day BETWEEN 1 AND 31),
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                            CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED')),
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_rental_dates CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_rentals_property_id ON rentals (property_id);
CREATE INDEX idx_rentals_tenant_id ON rentals (tenant_id);

-- Enforce: a property can only have ONE active rental at a time.
-- Partial unique index -- only applies to rows where status = 'ACTIVE'.
CREATE UNIQUE INDEX uq_rentals_one_active_per_property
    ON rentals (property_id)
    WHERE status = 'ACTIVE';

-- ---------- rent_payments ----------
CREATE TABLE rent_payments (
    id              BIGSERIAL PRIMARY KEY,
    rental_id       BIGINT NOT NULL REFERENCES rentals(id),
    period_month    DATE NOT NULL,
    amount_due      NUMERIC(12,2) NOT NULL CHECK (amount_due >= 0),
    amount_paid     NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (amount_paid >= 0),
    due_date        DATE NOT NULL,
    payment_date    DATE,
    status          VARCHAR(20) NOT NULL DEFAULT 'UNPAID'
                        CHECK (status IN ('PAID', 'PARTIALLY_PAID', 'UNPAID', 'OVERDUE')),
    payment_method  VARCHAR(20)
                        CHECK (payment_method IN ('CASH', 'BANK_TRANSFER', 'CHEQUE', 'ONLINE', 'OTHER')),
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_rent_payment_rental_month UNIQUE (rental_id, period_month)
);

CREATE INDEX idx_rent_payments_rental_id ON rent_payments (rental_id);
CREATE INDEX idx_rent_payments_period_month ON rent_payments (period_month);
