# Tenant rent history protection (V7)

## What changed
- Edit Tenant Terms updates the current monthly rent without modifying previous months' invoices, including **UNPAID** payments.
- Untouched invoices in the current month can be updated to the new rent; paid/partially paid or annotated invoices are preserved.
- New `tenant_rent_rates` table stores effective monthly rent rates by rental occupant; payment generation looks up the rate that applied to each month.
- Tenants/other occupants and their payment history are untouched when another tenant's rate changes.
- Start-date edits no longer auto-delete any past invoices. Missing invoices may be created at their month-appropriate rates.
- A rate change's effective month is restricted to the **current calendar month**. The UI shows that month. Backdated changes are rejected.

## Migration warning (production)
Flyway migration `V7__tenant_rent_rates.sql` is REQUIRED before/with the new backend deployment. It creates a separate rate-history table and seeds a baseline using the current stored rent for existing occupants. It does **not** overwrite payment amounts or alter `rent_payments` rows.

IMPORTANT: Past rent prices not already stored in an existing payment or historical record cannot be reconstructed reliably. This migration only knows the current rent when seeding old occupants. Review legacy ungenerated months before generating historical payments.

## Verification steps before merging/deploying
1. Back up Supabase PostgreSQL and test on a staging clone.
2. Run Flyway with the backend startup against staging; confirm V7 succeeds.
3. Edit one active occupant from 12,000 to 15,000 this month, with a prior month UNPAID invoice. Verify prior month's amount remains 12,000 and current untouched UNPAID amount becomes 15,000.
4. Check PAID/PARTIALLY_PAID invoices and payments with notes remain unchanged.
5. Generate payments; confirm older missing months use the stored historical rate, while current month uses 15,000.
6. Confirm other occupant's rent and invoices remain unchanged, and tenant profile shows the original payment amounts.
7. Verify changing rent with an effective month before the current month is rejected.
8. Run backend unit/integration tests and frontend production build in CI/staging, then deploy backend before frontend.

## Testing status
Structural source checks passed. Full Maven tests, frontend Vite build, and live database migration were **not executed** in this environment; production safety is not guaranteed. Deploy only after staging checks.
