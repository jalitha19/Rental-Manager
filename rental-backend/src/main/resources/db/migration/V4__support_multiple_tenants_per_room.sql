-- ============================================================
-- V4: Support multiple tenants per room and update due date logic
-- ============================================================

-- Add a second tenant column to rentals table
ALTER TABLE rentals ADD COLUMN tenant2_id BIGINT REFERENCES tenants(id);

-- Drop the old unique constraint that only allows one active rental per property
DROP INDEX IF EXISTS uq_rentals_one_active_per_property;

-- Add a new constraint ensuring only one active rental per property
-- A property can have at most 1 active rental, which can have 1-2 tenants
CREATE UNIQUE INDEX uq_rentals_one_active_per_property
    ON rentals (property_id)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_rentals_tenant2_id ON rentals (tenant2_id);

-- Add index for property_id + status to efficiently find all tenants in a room
CREATE INDEX idx_rentals_property_status ON rentals (property_id, status);
