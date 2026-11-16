# Rental Manager — Current rent on top, repeatable dated history below

This archive is the **complete Rental Manager project** based on `Rental-Manager-Rent-Period-History-Updated.zip`.

## What changed

The Rooms / Houses **Change tenant** screen and the Rentals **New rental** / **Change tenant** / **Edit tenant terms & rent history** screens now separate current rent from past rent prices:

- **Current / present rent at the top**: `Start date (DD/MM/YYYY)` (when today's rent price began) and `Monthly rent (Rs.)`.
- **Previous rent history underneath**: each row has `Start date (DD/MM/YYYY)`, `End date (DD/MM/YYYY)`, `Monthly rent (Rs.)`, and Remove.
- **+ Add next rent period** creates another complete history row each time, in chronological order.
- When editing a **retained/current tenant** in Change tenant, click **Edit this tenant’s rent history** first. The original move-in date and occupant identity remain untouched. The top date is now the date the *current price began*, not the original move-in date.
- When assigning a **new tenant** with old rental rates, the earliest dated history row determines their original move-in date. Without history, their original move-in date is simply the current top start date.
- Data is sent to the existing backend `rentPeriods` API format. The existing Flyway V7 table `tenant_rent_rates` still stores per-month rate transitions. No schema or backend API changes are needed.

## Example

For someone continuously staying since January 2018:

Top / current rent:

- Start date: `01/11/2025`
- Monthly rent: `7000`

Past period 1:

- Start date: `01/01/2018`
- End date: `31/07/2022`
- Monthly rent: `3500`

Past period 2 (use **+ Add next rent period**):

- Start date: `01/08/2022`
- End date: `31/10/2025`
- Monthly rent: `5000`

Result: the tenant keeps one continuous stay, the rate ledger has three effective rates, and future / explicitly requested missing invoices use the rate for their month.

## Important date and payment safeguards

- Rent is billed by **whole calendar months**, not individual days. A historical / effective rent period **must start on the first day of a month**, and historical periods **must end on the last day of the month**. Example: `01/08/2022` to `31/10/2025`. Use four-digit years to avoid ambiguity.
- Periods must be contiguous, chronological, without overlap, and must begin in the original stay's month for retained tenants. Future-effective rent changes aren't supported yet.
- The app never automatically changes or deletes past invoices when saving the rate timeline, including historical UNPAID invoices. An untouched current-month invoice may be reconciled by existing backend behavior.
- Historical missing invoice generation remains **opt-in** via an unchecked checkbox. If checked, missing invoices are created as UNPAID, never as Paid; this can create large overdue balances and must be confirmed against actual receipts.
- Existing tenants keep their original `rental_tenants.start_date` and stay ID. The present rent's start date and past rent periods change **rates**, not occupancy.
- This form does not automatically correct previously wrong invoice amounts; those require separately reviewed corrections.

## Deploying safely

This update modifies only these frontend files:

- `rental-frontend/src/utils/rentPeriods.js`
- `rental-frontend/src/utils/occupants.js`
- `rental-frontend/src/components/OccupantsForm.jsx`
- `rental-frontend/src/components/RentPeriodsEditor.jsx`
- `rental-frontend/src/components/OccupantTermsModal.jsx`

The **full backend is included but identical to the prior archive**. No new migration, Render deployment, database SQL, or environment variables are required **if the previous rent-period history backend is already deployed**. Otherwise, deploy that backend first.

Do not deploy a frontend that sends `rentPeriods` unless Render is running a backend version that already supports them. Use a staging copy of Supabase, test before production, and preserve your rollback branch and database backup.

### Checks performed here

- 9 targeted Node.js tests pass covering the three-period client example, repeated rows, full DD/MM/YYYY parsing and validation, existing-tenancy preservation, and backend payloads.
- All 39 JavaScript/JSX frontend files parsed without syntax errors.
- No backend code or Flyway migrations were changed.
- Full `npm run build` was **not** possible here: `npm ci --offline` lacks a cached `yallist` dependency. Full Maven/Java integration and live deployment tests were also **not** run.

Before production deployment, run `npm ci && npm run build` in `rental-frontend`, and test against a staging deployment with a sample tenant.
