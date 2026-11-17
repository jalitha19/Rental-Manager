# Fix: Change Tenant rejects backdated rent history

## Symptom
The Change Tenant form displayed:
`Change date cannot be before the current tenants' latest start date (2019-03-18)`
when an incoming tenant was assigned earlier historical rent periods (e.g. starting 2019-01-24).

## Root cause
`RentalService.changeTenant()` incorrectly derived the *changeover date* from the earliest `occupants[].startDate`. In the rent-history editor that date is intentionally the new occupant's historical move-in date. It must NOT automatically determine when an outgoing tenant's active stay ends.

## Fix
- Use the existing `ChangeTenantRequest.newStartDate` as the changeover date to end outgoing stays. The deployed frontend already sends today's date there.
- Keep an incoming occupant's original historical start date and exact rent periods, even when earlier than a departing tenant's start date.
- When an occupant stays, keep their existing `RentalTenant` identity, rent history and payment history; only an outgoing occupant is ended at the real changeover date.
- If the *actual changeover* date would end a departing tenant before they moved in, the safety check still rejects that impossible date.
- No payment amounts, payment statuses, DB tables, migrations, API routes or frontend files are changed.

## Important historical accuracy limitation
If a backfilled incoming stay predates an existing outgoing tenant's recorded start, the resulting historical rental **date intervals may overlap**. This patch intentionally does not delete or rewrite those existing records. Review the true move-in/move-out dates separately before treating such reports as an authoritative occupancy timeline.

## Deploy
1. Back up the production database and test on a staging copy.
2. Upload the changed backend `RentalService.java` to the same path in your GitHub `rental-backend/` folder; optional regression test file can be uploaded too.
3. Deploy the Render backend first; no frontend redeploy or new Flyway migration is required.
4. On a test room, set a historical rent start of 24/01/2019 where the previous occupant started 18/03/2019; saving should not show the *old* date-comparison error.
5. Verify the previous occupant's stay was ended on the intended changeover date (the existing UI currently sends today), and the incoming tenant kept the correct historical start. Check old and new payment records were not rewritten.

## Verification status
Source review and targeted static checks completed. Full Maven tests and integration testing against Supabase have not run in this environment; do not assume the change is production-verified.
