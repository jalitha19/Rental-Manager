import Field, { inputClass, btnSecondary } from './Field'
import { appendHistoricalPeriod, removeHistoricalPeriod, updateHistoricalPeriod } from '../utils/rentPeriods'

/** Only past price intervals live here. Current price and its start date stay in the fields above. */
export default function RentPeriodsEditor({ periods = [], onChange, readOnly = false }) {
  return (
    <div className="space-y-3 rounded-lg border border-border bg-ink/[0.025] p-3">
      <div>
        <p className="text-sm font-semibold text-ink">Previous rent history</p>
        <p className="text-xs text-ink-soft">Add each old price below. Use DD/MM/YYYY dates: start on the 1st and end on the last day of the month.</p>
      </div>
      {periods.length === 0 && <p className="text-xs text-ink-faint">No previous rent periods entered. The rent above is the current price.</p>}
      {periods.map((period, index) => (
        <div key={index} className="space-y-3 rounded-lg border border-border bg-surface p-3">
          <div className="flex items-center justify-between gap-2">
            <p className="text-xs font-semibold text-ink">Previous rent period {index + 1}</p>
            {!readOnly && <button type="button" className="text-xs font-medium text-danger" onClick={() => onChange(removeHistoricalPeriod(periods, index))}>Remove</button>}
          </div>
          <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
            <Field label="Start date (DD/MM/YYYY)">
              <input type="text" inputMode="numeric" autoComplete="off" placeholder="01/01/2018" className={inputClass}
                value={period.startDate} readOnly={readOnly} required
                onChange={(e) => onChange(updateHistoricalPeriod(periods, index, { startDate: e.target.value }))} />
            </Field>
            <Field label="End date (DD/MM/YYYY)">
              <input type="text" inputMode="numeric" autoComplete="off" placeholder="31/07/2022" className={inputClass}
                value={period.endDate} readOnly={readOnly} required
                onChange={(e) => onChange(updateHistoricalPeriod(periods, index, { endDate: e.target.value }))} />
            </Field>
          </div>
          <Field label="Monthly rent (Rs.)">
            <input type="number" min="0" step="0.01" className={inputClass}
              value={period.monthlyRent} readOnly={readOnly} required
              onChange={(e) => onChange(updateHistoricalPeriod(periods, index, { monthlyRent: e.target.value }))} />
          </Field>
        </div>
      ))}
      {!readOnly && <button type="button" className={btnSecondary} onClick={() => onChange(appendHistoricalPeriod(periods))}>
        + Add next rent period
      </button>}
      <p className="text-xs text-ink-soft">
        Rent is billed by calendar month. Previous periods must connect without gaps or overlaps and end before the current rent starts.
        Saving the timeline does not overwrite old invoices or mark rent as paid.
      </p>
    </div>
  )
}
