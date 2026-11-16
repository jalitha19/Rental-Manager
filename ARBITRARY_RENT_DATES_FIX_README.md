# Rental Manager – Real-calendar rent periods and gaps (V8)

## Client issue
The previous form only accepted rent intervals from the 1st through the last day of a month and required all periods to touch. This rejected real-life dates and gaps. The client also needed a calendar picker instead of manually typing dates.

## Fix
- **Current / present rent** stays at the top, with a date picker for the current rate's real start date and an amount.
- **Previous rent history** includes any number of rows, each with a start-date picker, end-date picker and monthly rent. Rows may have arbitrary days and gaps.
- Dates are stored exactly as selected (`YYYY-MM-DD` in the API/database); the UI also shows a `DD/MM/YYYY` hint. The browser's date input may display dates in the device's own locale.
- The editor sorts past periods on save. Only truly overlapping days, inverted dates, missing amounts and invalid dates are rejected.
- The latest/current period must be ongoing and must start no later than today.
- A continuing tenant keeps the same rental/occupancy record and their original move-in date.
- **No existing rent payment records are deleted or rewritten by the migration.** As before, saving can update an untouched current-month invoice; paid, partially paid and prior-month invoices remain protected.
- The optional **Generate missing payments** checkbox remains **OFF** by default. If enabled, missing monthly invoices are created as UNPAID using the rate applicable in that calendar month.

## IMPORTANT: Backend + database changes are required
The V7 `tenant_rent_rates` table cannot represent actual day ranges or gaps. This release adds a **V8 Flyway migration** that creates `tenant_rent_periods`, storing each interval's real start/end dates. Existing V7 records are left intact and remain readable for occupants who have not been edited using the new form. Once a new timeline is saved, V8 periods take precedence over the legacy V7 ledger for that occupant. Do not alter or rerun V7.

**Deploy order**: back up the production database; test with a staging database; deploy backend to Render and verify Flyway V8 succeeds; then deploy frontend to Vercel. **Do not deploy only the frontend.** Do not run this SQL manually on Supabase when Flyway is enabled. Check the Render service's configured migration permissions.

## Monthly billing semantics (important)
Each invoice still represents a **calendar month**, not daily rent. For a month with an exact-date interval, its amount is the monthly rent for the **earliest day in that month on which rent is defined** (and the occupant has moved in). When a date gap spans an entire month, automatic payment generation skips that month; it does not invent an amount or treat the tenant as moved out. For mid-month price changes, there is **no automatic proration or splitting of that month's invoice**. Review manually when the contract uses different rules. Period gaps do not alter room occupancy status.

## Production regression checks
1. Existing tenant with original move-in date: keep them selected, click **Edit this tenant's rent history**; verify the original date does not change.
2. Add past interval `12/01/2018`–`18/07/2022` Rs.3,500, then `09/08/2022`–`21/10/2025` Rs.5,000; current begins `17/11/2025` Rs.7,000. Saving should succeed despite gaps and mid-month boundaries.
3. Reopen the same tenant and check that **all exact calendar days** are preserved in both dropdown and Tenant Profile.
4. Confirm an intentionally overlapping interval is rejected but a gap isn't.
5. With optional invoice generation OFF, confirm no past payments are added or altered.
6. On staging only, with invoice generation ON, confirm months fully in a gap are skipped and all created invoices are UNPAID. Existing payment rows remain unchanged (except possibly the current-month eligible untouched invoice).
7. Check add/remove a second tenant: retaining an occupant does not restart their stay or replace their payments.
8. Confirm both Vercel and Render deploy successfully and Flyway reports schema version **8**.

## Verification limitations
Client-side unit tests are included at `rental-frontend/tests/rent-history-dates.test.mjs` and backend JUnit tests at `rental-backend/src/test/java/com/rental/service/TenantRentRateServiceScheduleTest.java`. The full Vite build, Maven test suite, and a real Supabase migration/integration test must be run in a staging/CI environment before production.
