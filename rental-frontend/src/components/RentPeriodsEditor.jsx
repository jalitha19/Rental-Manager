import Field, { inputClass, btnSecondary } from './Field'
import { appendHistoricalPeriod, displayDate, removeHistoricalPeriod, updateHistoricalPeriod } from '../utils/rentPeriods'

/** Historical prices are optional, individually dated intervals; gaps are valid. */
export default function RentPeriodsEditor({ periods = [], onChange, readOnly = false }) {
  return (
    <div className="space-y-3 rounded-lg border border-border bg-ink/[0.025] p-3">
      <div>
        <p className="text-sm font-semibold text-ink">Previous rent history</p>
        <p className="text-xs text-ink-soft">Choose any start and end date from the calendar. Gaps between periods are allowed; overlapping dates are not.</p>
      </div>
      {periods.length === 0 && <p className="text-xs text-ink-faint">No previous rent periods entered. The rent above is the current price.</p>}
      {periods.map((period, index) => (
        <div key={index} className="space-y-3 rounded-lg border border-border bg-surface p-3">
          <div className="flex items-center justify-between gap-2">
            <p className="text-xs font-semibold text-ink">Previous rent period {index + 1}</p>
            {!readOnly && <button type="button" className="text-xs font-medium text-danger" onClick={() => onChange(removeHistoricalPeriod(periods, index))}>Remove</button>}
          </div>
          <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
            <Field label="Start date" hint={period.startDate ? `Selected: ${displayDate(period.startDate)}` : 'Choose from calendar'}>
              <input type="date" className={inputClass} value={period.startDate || ''} readOnly={readOnly} required
                onChange={(e) => onChange(updateHistoricalPeriod(periods, index, { startDate: e.target.value }))} />
            </Field>
            <Field label="End date" hint={period.endDate ? `Selected: ${displayDate(period.endDate)}` : 'Choose from calendar'}>
              <input type="date" className={inputClass} value={period.endDate || ''} readOnly={readOnly} required
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
        A gap is saved as a period with no defined rent; generating payments skips fully uncovered months.
        If a rent changes mid-month, that month uses the first applicable price (no automatic proration).
        Saving history does not overwrite existing payment records or mark them paid.
      </p>
    </div>
  )
}
