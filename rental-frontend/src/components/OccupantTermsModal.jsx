import { useEffect, useState } from 'react'
import { updateRental } from '../services/rentalService'
import { getErrorMessage } from '../services/api'
import { getOccupants } from '../utils/occupants'
import { buildRentSchedule, displayDate, scheduleToForm, validateRentHistory } from '../utils/rentPeriods'
import { useToast } from '../contexts/ToastContext'
import Modal from './Modal'
import RentPeriodsEditor from './RentPeriodsEditor'
import Field, { inputClass, btnPrimary, btnSecondary } from './Field'

export default function OccupantTermsModal({ rental, onClose, onSaved }) {
  const { push } = useToast()
  const [rows, setRows] = useState([])
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (!rental) { setRows([]); return }
    setRows(getOccupants(rental).map((occupant) => ({
      id: occupant.id,
      name: occupant.tenant?.fullName ?? '',
      dirty: false,
      generateMissingPayments: false,
      // The original stay remains separate from the start of the current rent.
      startDate: String(occupant.startDate ?? '').slice(0, 10),
      ...scheduleToForm(occupant.rentPeriods, occupant.startDate, occupant.monthlyRent),
    })))
  }, [rental])

  function update(index, patch) {
    setRows((current) => current.map((row, i) => i === index ? { ...row, ...patch, dirty: true } : row))
  }

  async function submit(e) {
    e.preventDefault()
    const changed = rows.filter((row) => row.dirty)
    if (!changed.length) return
    try {
      const occupants = changed.map((row) => {
        const schedule = buildRentSchedule({ stayStart: row.startDate,
          currentStartText: row.currentStartText, monthlyRent: row.monthlyRent,
          historicalPeriods: row.historicalPeriods, retained: true })
        return { id: row.id, startDate: row.startDate,
          monthlyRent: Number(row.monthlyRent), rentPeriods: schedule.rentPeriods,
          generateMissingPayments: !!row.generateMissingPayments }
      })
      setSaving(true)
      await updateRental(rental.id, {
        endDate: rental.status === 'ACTIVE' ? null : rental.endDate ? String(rental.endDate).slice(0, 10) : null,
        deposit: rental.deposit ?? null,
        paymentDueDay: rental.paymentDueDay ?? null,
        notes: rental.notes ?? null,
        occupants,
      })
      push('Rent history saved. Earlier payment records were preserved.')
      onClose()
      if (onSaved) await onSaved()
    } catch (err) {
      push(getErrorMessage(err), 'error')
    } finally { setSaving(false) }
  }

  return (
    <Modal open={!!rental} title="Edit tenant terms & rent history" onClose={onClose} wide>
      <form onSubmit={submit} className="space-y-4">
        <p className="text-sm text-ink-soft">
          The top fields are the current rent and when it began. Enter older rent periods below with their own start date, end date and amount.
          Saving will not rewrite past monthly invoices.
        </p>
        {rows.map((row, index) => {
          const error = row.dirty ? validateRentHistory({ stayStart: row.startDate,
            currentStartText: row.currentStartText, monthlyRent: row.monthlyRent,
            historicalPeriods: row.historicalPeriods, retained: true }) : ''
          return (
            <div key={row.id ?? index} className="space-y-3 rounded-xl border border-border p-3">
              <p className="text-sm font-semibold text-ink">{row.name}</p>
              <Field label="Original tenant move-in date" hint="This is not the rent increase date; keep it unchanged unless the move-in date was entered incorrectly.">
                <input type="date" className={inputClass} value={row.startDate} required
                  onChange={(e) => update(index, { startDate: e.target.value })} />
              </Field>
              <div className="space-y-2 rounded-lg border border-border bg-ink/[0.025] p-3">
                <p className="text-xs font-semibold text-ink">Current / present rent</p>
                <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                  <Field label="Current rent start date" hint={`When today's rent began${row.currentStartText ? `: ${displayDate(row.currentStartText)}` : ''}`}>
                    <input type="date"
                      className={inputClass} value={row.currentStartText} required
                      onChange={(e) => update(index, { currentStartText: e.target.value })} />
                  </Field>
                  <Field label="Monthly rent (Rs.)">
                    <input type="number" min="0" step="0.01" className={inputClass}
                      value={row.monthlyRent} required
                      onChange={(e) => update(index, { monthlyRent: e.target.value })} />
                  </Field>
                </div>
              </div>
              <RentPeriodsEditor periods={row.historicalPeriods}
                onChange={(historicalPeriods) => update(index, { historicalPeriods })} />
              {error && <p className="text-xs text-danger" role="alert">{error}</p>}
              <label className="flex items-start gap-2 text-xs text-ink-soft">
                <input className="mt-0.5" type="checkbox" checked={row.generateMissingPayments}
                  onChange={(e) => update(index, { generateMissingPayments: e.target.checked })} />
                <span>Also generate missing invoices as UNPAID. Only use after checking old receipts; this can add overdue balances.</span>
              </label>
            </div>
          )
        })}
        <div className="flex justify-end gap-2 pt-2">
          <button type="button" className={btnSecondary} onClick={onClose}>Cancel</button>
          <button type="submit" className={btnPrimary} disabled={saving || !rows.some((r) => r.dirty) || rows.some((r) => r.dirty && (r.id == null || !!validateRentHistory({
            stayStart: r.startDate, currentStartText: r.currentStartText, monthlyRent: r.monthlyRent,
            historicalPeriods: r.historicalPeriods, retained: true,
          })))}>{saving ? 'Saving…' : 'Save rent history'}</button>
        </div>
      </form>
    </Modal>
  )
}
