# Tenant rent price period history (new feature)

This is the complete `Rental-Manager` frontend + backend based on the prior **Rent-History-Protected** version.

## Client example

One ongoing rental stay beginning 2018-01-17 can have these *monthly rate periods*:

| Start month | End month, inclusive | Monthly rent |
|---|---|---:|
| Jan 2018 | Jul 2022 | Rs. 3,500 |
| Aug 2022 | Oct 2025 | Rs. 5,000 |
| Nov 2025 | Present | Rs. 7,000 |

The original occupant's `rental_tenants` record (and its `start_date`) stays the same; **no new rental is created merely because rent increased**. `monthly_rent` stores the latest rate. The full sequence is recorded per occupant in the *existing* `tenant_rent_rates` table, with effective month transitions (each end month is inferred from the next start month).

## Where to use it

- **Rooms / Houses → Assign tenant / Change tenant:** set tenant + original start date; click **Add historical rent price periods**, then **Add next rent period** as often as necessary. For a continuing tenant, use **Edit this tenant's rent price history** without changing the selected tenant.
- **Rentals → New rental:** the same per-occupant editor is available.
- **Rentals → Edit tenant terms & rent history:** edit a current tenant's saved period rows. Only edited occupants are submitted. An occupant who stays keeps their rental identity.
- **Tenants → Tenant profile → Rental history:** displays the saved rent price timeline alongside the start/end of the stay.

First period must start in the same *calendar month* as the tenant's original start date. Historical months are inclusive. Each next price must start the following month: **no gaps or overlaps**. The final period runs to Present. Future-effective periods are not yet supported, because an early activation could incorrectly change today's displayed rent.

## Payment handling — production data safety

- **Existing past invoices are never automatically changed or deleted**, even if they are currently UNPAID. Paid, partially paid, and annotated records are also preserved.
- The **current month's untouched UNPAID invoice** can be updated to the latest rate; invoices with payment activity or notes are protected.
- Rate editing does not automatically assert which historical months were paid.
- **Creating missing UNPAID invoices is an explicit opt-in checkbox per occupant**, unchecked by default. If selected, the backend generates only missing monthly invoice records through the current month, using the applicable period-specific rate, and does **not** mark them as paid. This may create years of overdue balances, so check previous receipts first. Without opt-in, use the existing Rent Payments page's manual month-generation workflow if desired (which also backfills missing months).
- A payment record with the wrong old amount must be reviewed/corrected separately; storing a rate history does not silently overwrite historical accounting entries.
- The existing per-occupant payment unique index prevents duplicate invoices for the same month. The backend also checks for existing invoices before generation.

## Database and deployment

**No new SQL migration is necessary**. This uses the deployed Flyway V7 `tenant_rent_rates` table. The original `V7__tenant_rent_rates.sql` file was **not modified**, preserving Flyway checksums. No environment variable or authentication change is required.

**Do not deploy directly to the production branch without a staging test.**

1. Make a full restorable backup of Supabase (a CSV alone is not a database backup).
2. Test on a staging copy of data. In particular, try a 3-period 2018/2022/2025 example, with some PAID and UNPAID invoices already present, and confirm older existing `amount_due` values are unchanged.
3. Deploy the updated **backend to Render first**, and verify service startup. Since no V8 migration exists, do not expect Flyway V8 logs.
4. Deploy the updated **frontend to Vercel**. Check `VITE_API_URL` still points to the correct backend.
5. Test the Rooms change-tenant modal, adding a second tenant without resetting the first, Rates editor, Tenant profile, and current/future payment generation.
6. Before selecting the historical unpaid backfill checkbox in production, verify the tenant's old receipts and existing invoices. Otherwise leave it unchecked.

## Verification performed in this workspace

- Node tests exercised the user example and verified adjacent month boundaries and gap/overlap rejection.
- TypeScript parser successfully parsed the JSX/JS frontend files.
- Java 21 compiler parser successfully parsed all backend main + test Java files.
- Regression JUnit tests were added for three-period rate selection, validation, and historical payment preservation.
- **Full Maven/JUnit and Vite builds, database integration tests, and deployment tests were not completed here** (Maven is unavailable; npm package installation was incomplete). They must be run before production rollout.
