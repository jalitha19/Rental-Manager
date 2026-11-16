# Tenant change fix — deployment and verification

This is the **full frontend and backend source** with a focused change to the Change tenant feature. It does **not** change any Flyway migration, API route, authentication, or deployment configuration.

## Updated behavior
- Rooms/Houses and Rentals -> Change tenant preselects the current tenant(s), including their original start dates and rent values.
- Add second tenant keeps the current tenant and their original rental-tenant ID and payment records intact.
- Replacing one of two tenants keeps the other occupant's record intact, marks the departing person's occupancy end date, and creates a new occupant record for the newcomer.
- Removing the second tenant marks their stay ended without deleting their payment history.
- Replacing everybody uses the existing close-old-rental/create-new-rental behavior.
- Room and current-rental displays filter out past occupants, while monthly reports and history can still read historical occupant records.
- Continuing tenants' start dates and rent values are intentionally not changed by Change tenant; use Edit tenant terms for legitimate rent adjustments.

## Important behavior detail
When at least one tenant stays, the existing rental ID is intentionally preserved. The rental's `rental_tenants` records now include both current and past individual occupants; past people have a non-null `end_date`. This avoids resetting a continuing person's payment relationship. Old rental histories remain accessible via their original occupants.

## Local checks

```
cd rental-frontend
npm ci
npm run build

cd ../rental-backend
mvn test
mvn clean package -DskipTests
```

Java 21 and Maven are required for the backend. The backend regression test `RentalServiceChangeTenantTest` covers both addition and single-occupant replacement.

## Verify in staging (with copied test data; never use production tenant data casually)
1. Room with one tenant: open Change tenant; confirm that tenant is preselected with original start date and rent. Save without changing them; no original payment/occupancy ID should be altered.
2. In the same room, Add second tenant with today's date; verify both tenants appear and the first person's history, payments, start date and rent remain unchanged.
3. Room with two tenants: change the second dropdown to a different tenant; verify first occupant unchanged, outgoing occupant marked ended, newcomer current, and prior payments still belong to the correct people.
4. Remove second tenant; verify the remaining person stays active and outgoing person remains in history.
5. Change all tenants; verify old rental becomes completed and the new rental is active.
6. Check Dashboard, payment generation, Rent Payments, tenant details, and monthly rental print reports before production deployment.
7. Verify frontend and backend builds; back up your Supabase database, stage the rollout, and retain a known-good Git commit for rollback.

## Deployment order
Deploy and validate **Render backend first**, then **Vercel frontend**. The API path and DTO remain compatible with the old client. Make sure production environment variables retain their current values. No Flyway migration is required.

## Validation status
- Frontend occupant utility assertions: passed in the provided environment.
- Maven unit tests: added but could not execute here (Maven unavailable).
- Full frontend Vite build: could not execute here (`npm ci --offline` lacks cached dependencies).
- Production database/UI and live service integration: not tested. Do not deploy directly to production without staging verification.
