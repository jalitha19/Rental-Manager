import Field, { inputClass, btnSecondary } from './Field'
import { emptyOccupant } from '../utils/occupants'
import { formatCurrency } from '../utils/format'

export default function OccupantsForm({ tenants, occupants, onChange, standardRent }) {
  const hasStandardRent = standardRent !== undefined && standardRent !== null && standardRent !== ''

  function update(index, patch) {
    onChange(occupants.map((occupant, i) => (i === index ? { ...occupant, ...patch } : occupant)))
  }

  function addSecond() {
    const first = occupants[0]
    onChange([
      ...occupants,
      emptyOccupant(first?.startDate ?? '', hasStandardRent ? standardRent : first?.monthlyRent ?? ''),
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
                onChange={(e) => update(index, { tenantId: e.target.value })}
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
                  onChange={(e) => update(index, { startDate: e.target.value })}
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
                  onChange={(e) => update(index, { monthlyRent: e.target.value })}
                  required
                />
              </Field>
            </div>
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
