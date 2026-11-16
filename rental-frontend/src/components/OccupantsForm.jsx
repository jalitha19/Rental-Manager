import Field, { inputClass, btnSecondary } from './Field'
import { emptyOccupant } from '../utils/occupants'
import { formatCurrency, formatDate, todayIso } from '../utils/format'
import { displayDate, scheduleToForm, validateRentHistory } from '../utils/rentPeriods'
import RentPeriodsEditor from './RentPeriodsEditor'

export default function OccupantsForm({ tenants, occupants, onChange, standardRent, changing = false }) {
  const hasStandardRent = standardRent !== undefined && standardRent !== null && standardRent !== ''
  function update(index, patch) {
    onChange(occupants.map((occupant, i) => i === index ? { ...occupant, ...patch } : occupant))
  }
  function addSecond() {
    const first = occupants[0]
    onChange([...occupants, emptyOccupant(changing ? todayIso() : first?.startDate ?? '',
      hasStandardRent ? standardRent : first?.monthlyRent ?? '')])
  }
  return (
    <div className="space-y-3">
      {occupants.map((occupant, index) => {
        const otherId = occupants[index === 0 ? 1 : 0]?.tenantId
        const isRetained = changing && !!occupant.originalTenantId && String(occupant.tenantId) === String(occupant.originalTenantId)
        const editing = !isRetained || !!occupant.editRetainedSchedule
        const error = editing && occupant.tenantId
          ? validateRentHistory({ stayStart: occupant.startDate, currentStartText: occupant.currentStartText,
              monthlyRent: occupant.monthlyRent, historicalPeriods: occupant.historicalPeriods,
              retained: isRetained }) : ''
        return (
          <div key={index} className="space-y-3 rounded-lg border border-border p-3">
            <div className="flex items-center justify-between">
              <p className="text-sm font-semibold text-ink">{index === 0 ? 'First tenant' : 'Second tenant'}</p>
              {index === 1 && <button type="button" className="text-xs font-medium text-danger"
                onClick={() => onChange(occupants.slice(0, 1))}>Remove</button>}
            </div>
            <Field label="Tenant">
              <select className={inputClass} value={occupant.tenantId} required onChange={(e) => {
                const tenantId = e.target.value
                const original = changing && tenantId === occupant.originalTenantId
                const date = original ? occupant.originalStartDate : (changing ? todayIso() : occupant.startDate)
                const rent = original ? occupant.originalMonthlyRent : occupant.monthlyRent
                update(index, { tenantId, startDate: date, ...scheduleToForm([], date, rent),
                  editRetainedSchedule: false, generateMissingPayments: false })
              }}>
                <option value="">Select tenant</option>
                {tenants.filter((t) => !otherId || String(t.id) !== String(otherId))
                  .map((t) => <option key={t.id} value={t.id}>{t.fullName}</option>)}
              </select>
            </Field>
            <div className="rounded-lg border border-border bg-ink/[0.025] p-3 space-y-2">
              <p className="text-xs font-semibold text-ink">Current / present rent</p>
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <Field label="Start date (DD/MM/YYYY)" hint="When the current rent price began">
                  <input type="text" inputMode="numeric" autoComplete="off" placeholder="01/11/2025"
                    className={inputClass} value={occupant.currentStartText ?? displayDate(occupant.startDate)}
                    readOnly={!editing} required
                    onChange={(e) => update(index, { currentStartText: e.target.value })} />
                </Field>
                <Field label="Monthly rent (Rs.)">
                  <input type="number" min="0" step="0.01" className={inputClass}
                    value={occupant.monthlyRent} readOnly={!editing} required
                    onChange={(e) => update(index, { monthlyRent: e.target.value })} />
                </Field>
              </div>
              {isRetained && <p className="text-xs text-ink-faint">Tenant's original move-in date stays {formatDate(occupant.startDate)}. Changing their rent does not restart their stay.</p>}
              {isRetained && <button type="button" className="text-xs font-medium text-ink-soft underline"
                onClick={() => {
                  if (!occupant.editRetainedSchedule) {
                    update(index, { editRetainedSchedule: true, ...scheduleToForm(occupant.existingRentPeriods, occupant.startDate, occupant.monthlyRent) })
                  } else {
                    update(index, { editRetainedSchedule: false, generateMissingPayments: false,
                      ...scheduleToForm(occupant.existingRentPeriods, occupant.startDate, occupant.originalMonthlyRent) })
                  }
                }}>{editing ? 'Cancel editing this tenant’s rent' : 'Edit this tenant’s rent history'}</button>}
            </div>
            <RentPeriodsEditor periods={occupant.historicalPeriods ?? []} readOnly={!editing}
              onChange={(historicalPeriods) => update(index, { historicalPeriods })} />
            {editing && error && <p className="text-xs text-danger" role="alert">{error}</p>}
            {editing && <label className="flex items-start gap-2 text-xs text-ink-soft">
              <input type="checkbox" className="mt-0.5" checked={!!occupant.generateMissingPayments}
                onChange={(e) => update(index, { generateMissingPayments: e.target.checked })} />
              <span>Also create missing monthly payment records as <strong>UNPAID</strong> through this month. Only select this after verifying old payments; many overdue invoices may be created.</span>
            </label>}
            {hasStandardRent && index === 0 && <p className="text-xs text-ink-faint">Standard rent for this property: {formatCurrency(standardRent)}</p>}
          </div>
        )
      })}
      {occupants.length < 2 && <button type="button" className={btnSecondary} onClick={addSecond}>Add second tenant</button>}
    </div>
  )
}
