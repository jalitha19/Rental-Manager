-- Store precise date intervals without changing the established V7 rent ledger or invoices.
-- Existing V7 rows are read as legacy history until explicitly replaced using the new editor.
CREATE TABLE tenant_rent_periods (
    id BIGSERIAL PRIMARY KEY,
    rental_tenant_id BIGINT NOT NULL REFERENCES rental_tenants(id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    end_date DATE,
    monthly_rent NUMERIC(12,2) NOT NULL CHECK (monthly_rent >= 0),
    CONSTRAINT ck_tenant_rent_period_dates CHECK (end_date IS NULL OR end_date >= start_date),
    CONSTRAINT uq_tenant_rent_period_start UNIQUE (rental_tenant_id, start_date)
);
CREATE INDEX idx_tenant_rent_period_dates ON tenant_rent_periods(rental_tenant_id, start_date, end_date);
