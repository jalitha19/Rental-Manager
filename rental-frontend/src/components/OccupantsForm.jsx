import Field, { inputClass, btnSecondary } from './Field'
import { emptyOccupant } from '../utils/occupants'
import { formatCurrency, todayIso } from '../utils/format'
import { initialRentPeriods, monthOf } from '../utils/rentPeriods'
import RentPeriodsEditor from './RentPeriodsEditor'

export default function OccupantsForm({ tenants, occupants, onChange, standardRent, changing = false }) {
  const hasStandardRent = standardRent !== undefined && standardRent !== null && standardRent !== ''

  function update(index, patch) {
    onChange(occupants.map((occupant, i) => (i === index ? { ...occupant, ...patch } : occupant)))
  }

  function addSecond() {
    const first = occupants[0]
    onChange([
      ...occupants,
      emptyOccupant(changing ? todayIso() : first?.startDate ?? '', hasStandardRent ? standardRent : first?.monthlyRent ?? ''),
    ])
  }

  function removeSecond() {
    onChange(occupants.slice(0, 1))
  }

  return (
    <div className="space-y-3">
      {occupants.map((occupant, index) => {
        const otherId = occupants[index === 0 ? 1 : 0]?.tenantId
        return (
          <div key={index} className="space-y-3 rounded-lg border border-border p-3">
            <div className="flex items-center justify-between">
              <p className="text-sm font-semibold text-ink">{index === 0 ? 'First tenant' : 'Second tenant'}</p>
              {index === 1 && (
                <button type="button" className="text-xs font-medium text-danger" onClick={removeSecond}>
                  Remove
                </button>
              )}
            </div>
            <Field label="Tenant">
              <select
                className={inputClass}
                value={occupant.tenantId}
                onChange={(e) => {
                  const tenantId = e.target.value
                  const isOriginal = changing && tenantId === occupant.originalTenantId
                  update(index, {
                    tenantId,
                    startDate: changing ? (isOriginal ? occupant.originalStartDate : todayIso()) : occupant.startDate,
                    rentPeriods: [],
                    editRetainedSchedule: false,
                    generateMissingPayments: false,
                  })
                }}
                required
              >
                <option value="">Select tenant</option>
                {tenants
                  .filter((t) => !otherId || String(t.id) !== String(otherId))
                  .map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.fullName}
                    </option>
                  ))}
              </select>
            </Field>
            <div className="grid grid-cols-2 gap-3">
              <Field label="Start date">
                <input
                  type="date"
                  className={inputClass}
                  value={occupant.startDate}
                  readOnly={changing && String(occupant.tenantId) === occupant.originalTenantId}
                  onChange={(e) => update(index, {
                    startDate: e.target.value,
                    rentPeriods: occupant.rentPeriods?.length
                      ? [{ ...occupant.rentPeriods[0], startMonth: monthOf(e.target.value) }, ...occupant.rentPeriods.slice(1)]
                      : occupant.rentPeriods,
                  })}
                  required
                />
              </Field>
              <Field label="Monthly rent (Rs.)">
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  className={inputClass}
                  value={occupant.monthlyRent}
                  readOnly={(changing && String(occupant.tenantId) === occupant.originalTenantId) || !!occupant.rentPeriods?.length}
                  onChange={(e) => update(index, { monthlyRent: e.target.value })}
                  required
                />
              </Field>
            </div>
            {changing && occupant.originalTenantId && String(occupant.tenantId) === occupant.originalTenantId && !occupant.editRetainedSchedule ? (
              <div className="space-y-2">
                <p className="text-xs text-ink-faint">Existing stay is preserved. You can edit only its rent price timeline without replacing the tenant.</p>
                <button type="button" className="text-xs font-medium text-ink-soft underline"
                  onClick={() => update(index, {
                    editRetainedSchedule: true,
                    rentPeriods: occupant.existingRentPeriods?.length
                      ? occupant.existingRentPeriods.map((p) => ({ ...p }))
                      : initialRentPeriods(occupant.startDate, occupant.monthlyRent),
                  })}>
                  + Edit this tenant's rent price history
                </button>
              </div>
            ) : occupant.rentPeriods?.length ? (
              <RentPeriodsEditor
                periods={occupant.rentPeriods}
                stayStart={occupant.startDate}
                onChange={(periods) => update(index, {
                  rentPeriods: periods,
                  monthlyRent: periods[periods.length - 1]?.monthlyRent ?? occupant.monthlyRent,
                })}
                allowDisable
                onDisable={() => update(index, {
                  editRetainedSchedule: false,
                  generateMissingPayments: false,
                  rentPeriods: [],
                  monthlyRent: occupant.originalTenantId && String(occupant.tenantId) === occupant.originalTenantId
                    ? occupant.existingRentPeriods?.at(-1)?.monthlyRent ?? occupant.monthlyRent
                    : occupant.monthlyRent,
                })}
              />
            ) : (
              <button type="button" className="text-xs font-medium text-ink-soft underline"
                onClick={() => update(index, { rentPeriods: initialRentPeriods(occupant.startDate, occupant.monthlyRent) })}>
                + Add historical rent price periods
              </button>
            )}
            {occupant.rentPeriods?.length > 0 && (
              <label className="flex items-start gap-2 text-xs text-ink-soft">
                <input type="checkbox" className="mt-0.5" checked={!!occupant.generateMissingPayments}
                  onChange={(e) => update(index, { generateMissingPayments: e.target.checked })} />
                <span>Also create missing monthly payment records as <strong>UNPAID</strong> through the current month.
                  Only select this after reviewing past payments; this can add many overdue invoices.</span>
              </label>
            )}
            {hasStandardRent && index === 0 && (
              <p className="text-xs text-ink-faint">Standard rent for this property: {formatCurrency(standardRent)}</p>
            )}
          </div>
        )
      })}
      {occupants.length < 2 && (
        <button type="button" className={btnSecondary} onClick={addSecond}>
          Add second tenant
        </button>
      )}
    </div>
  )
}
