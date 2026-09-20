CREATE TABLE rental_tenants (
    id              BIGSERIAL PRIMARY KEY,
    rental_id       BIGINT NOT NULL REFERENCES rentals(id) ON DELETE CASCADE,
    tenant_id       BIGINT NOT NULL REFERENCES tenants(id),
    start_date      DATE NOT NULL,
    end_date        DATE,
    monthly_rent    NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (monthly_rent >= 0),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_rental_tenant_dates CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_rental_tenants_rental_id ON rental_tenants (rental_id);
CREATE INDEX idx_rental_tenants_tenant_id ON rental_tenants (tenant_id);

INSERT INTO rental_tenants (rental_id, tenant_id, start_date, end_date, monthly_rent)
SELECT id, tenant_id, start_date, end_date, monthly_rent
FROM rentals
ORDER BY id;

INSERT INTO rental_tenants (rental_id, tenant_id, start_date, end_date, monthly_rent)
SELECT id, tenant2_id, start_date, end_date, 0
FROM rentals
WHERE tenant2_id IS NOT NULL
ORDER BY id;

ALTER TABLE rent_payments
    ADD COLUMN rental_tenant_id BIGINT REFERENCES rental_tenants(id);

UPDATE rent_payments p
SET rental_tenant_id = (
    SELECT MIN(rt.id)
    FROM rental_tenants rt
    WHERE rt.rental_id = p.rental_id
);

CREATE INDEX idx_rent_payments_rental_tenant_id ON rent_payments (rental_tenant_id);

ALTER TABLE rent_payments DROP CONSTRAINT uq_rent_payment_rental_month;

CREATE UNIQUE INDEX uq_rent_payment_occupant_month
    ON rent_payments (rental_tenant_id, period_month)
    WHERE rental_tenant_id IS NOT NULL;
