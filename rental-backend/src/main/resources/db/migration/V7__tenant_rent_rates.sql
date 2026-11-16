-- Historical monthly rent rates per rental occupant. Existing invoice amounts are not modified.
CREATE TABLE tenant_rent_rates (
    id BIGSERIAL PRIMARY KEY,
    rental_tenant_id BIGINT NOT NULL REFERENCES rental_tenants(id) ON DELETE CASCADE,
    effective_month DATE NOT NULL,
    monthly_rent NUMERIC(12,2) NOT NULL CHECK (monthly_rent >= 0),
    CONSTRAINT uq_tenant_rent_rate_month UNIQUE (rental_tenant_id, effective_month)
);
CREATE INDEX idx_tenant_rent_rates_lookup ON tenant_rent_rates(rental_tenant_id, effective_month);
-- Existing rentals have no stored historical rate changes; baseline is the rate currently known.
INSERT INTO tenant_rent_rates (rental_tenant_id, effective_month, monthly_rent)
SELECT id, DATE_TRUNC('month', start_date)::date, monthly_rent FROM rental_tenants;
