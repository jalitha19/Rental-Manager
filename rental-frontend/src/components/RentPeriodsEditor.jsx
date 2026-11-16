import Field, { inputClass, btnSecondary } from './Field'
import { appendRentPeriod, removeRentPeriod, updateRentPeriod, validateRentPeriods } from '../utils/rentPeriods'

/** Repeatable monthly price segments for a single, unchanged tenant stay. */
export default function RentPeriodsEditor({ periods, onChange, stayStart, allowDisable = false, onDisable }) {
  const error = validateRentPeriods(periods, stayStart)
  return (
    <div className="space-y-3 rounded-lg border border-border bg-ink/[0.025] p-3">
      <div className="flex items-center justify-between gap-3">
        <div>
          <p className="text-sm font-semibold text-ink">Rent price history</p>
          <p className="text-xs text-ink-soft">Each price covers entire calendar months. The last price continues to Present.</p>
        </div>
        {allowDisable && <button type="button" className="text-xs text-ink-soft underline" onClick={onDisable}>Use one rent only</button>}
      </div>
      {periods.map((period, index) => (
        <div key={index} className="space-y-2 rounded-lg border border-border bg-surface p-3">
          <div className="flex items-center justify-between">
            <p className="text-xs font-semibold text-ink">{index === periods.length - 1 ? 'Current / latest price' : `Historical price ${index + 1}`}</p>
            {periods.length > 1 && index > 0 && (
              <button type="button" className="text-xs text-danger" onClick={() => onChange(removeRentPeriod(periods, index))}>Remove</button>
            )}
          </div>
          <div className="grid grid-cols-2 gap-2">
            <Field label="From month">
              <input type="month" className={inputClass} value={period.startMonth}
                readOnly required />
            </Field>
            <Field label="Through month">
              {index === periods.length - 1
                ? <input value="Present / ongoing" className={inputClass} readOnly aria-label="Through present" />
                : <input type="month" className={inputClass} value={period.endMonth}
                    min={period.startMonth} onChange={(e) => onChange(updateRentPeriod(periods, index, { endMonth: e.target.value }))} required />}
            </Field>
          </div>
          <Field label="Monthly rent (Rs.)">
            <input type="number" className={inputClass} min="0" step="0.01" required value={period.monthlyRent}
              onChange={(e) => onChange(updateRentPeriod(periods, index, { monthlyRent: e.target.value }))} />
          </Field>
        </div>
      ))}
      <button type="button" className={btnSecondary} onClick={() => onChange(appendRentPeriod(periods))}>
        + Add next rent period
      </button>
      {error && <p className="text-xs text-danger" role="alert">{error}</p>}
      <p className="text-xs text-ink-soft">
        The price history determines the amount for future invoices and any missing invoices you explicitly choose to generate.
        Past-month invoices are never automatically changed; only an untouched current-month invoice may be updated. Record actual past payments separately.
      </p>
    </div>
  )
}
