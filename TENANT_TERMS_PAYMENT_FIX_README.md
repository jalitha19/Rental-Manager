# Tenant terms / payment history synchronization fix

This is the complete source project based on `Rental-Manager-Tenant-Change-Fixed.zip`.

## Problem
- `RentalService.update` saved each rental occupant's new start date and monthly rent without synchronizing monthly rent payment records.
- Tenant profile payment history must match the correct occupant record, including second occupants and former tenants.

## New behavior
- Saving Edit tenant terms synchronizes outstanding, untouched UNPAID invoices for that occupant: rent amount and due date are recalculated.
- If start date is moved later, automatically generated UNPAID invoices before the new first month are deleted only when they contain no payments, payment date/method, or notes.
- Paid, partially paid, manually annotated, and other non-untouched records are preserved without changing their values, even if they are before the corrected start date. Review them manually on the Payments page if correcting historical entries.
- For ACTIVE rentals, missing unpaid monthly records are created from the updated start month through the current month for occupants with positive monthly rent. Completed rentals do not auto-create invoices.
- Tenant profile filters payment records by occupant (`rentalTenantId`) using rental history, rather than relying on a potentially stale primary tenant summary. Legacy payments still use the existing fallback.
- No database migration, endpoint change, or environment variable change.

## Safety checklist before deployment
1. Back up Supabase Postgres and keep the current deployment/Git commit available for rollback.
2. In staging, start with a tenant with UNPAID invoices. Adjust start date and monthly rent, save, and verify Payments and tenant profile show the updated months and amounts.
3. Verify fully paid and partially paid historic payments are unchanged; check annotated invoices as well.
4. Verify a second occupant's payments appear only under their tenant profile.
5. Test adding/replacing a tenant and run Java `mvn test` and frontend `npm ci && npm run build` in an environment with dependencies.
6. Deploy the Render backend first; deploy the Vercel frontend only after verifying API responses.

IMPORTANT: Full Java tests and production/staging integration tests could not be run in the editing environment. Do not deploy to production without running them.
