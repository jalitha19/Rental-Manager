-- Tracks the standard/listed rent of a house or room over time, so raising
-- or lowering it keeps the earlier rate on record instead of overwriting it.
-- This is separate from what any tenant actually pays (rental_tenants.monthly_rent),
-- which already has its own history and is not affected by this table.
CREATE TABLE property_rent_history (
    id              BIGSERIAL PRIMARY KEY,
    property_id     BIGINT NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    monthly_rent    NUMERIC(12,2) NOT NULL CHECK (monthly_rent >= 0),
    effective_from  DATE NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_property_rent_history_property_id ON property_rent_history (property_id);

-- Seed one history row per existing property, using its current rent and
-- the date the property was added, so every property has a starting point.
INSERT INTO property_rent_history (property_id, monthly_rent, effective_from)
SELECT id, monthly_rent, created_at::date
FROM properties
ORDER BY id;
