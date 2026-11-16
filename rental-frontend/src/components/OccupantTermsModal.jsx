import { useEffect, useState } from 'react'
import { updateRental } from '../services/rentalService'
import { getErrorMessage } from '../services/api'
import { getOccupants } from '../utils/occupants'
import { initialRentPeriods, monthOf, rentPeriodsPayload, validateRentPeriods } from '../utils/rentPeriods'
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
      startDate: String(occupant.startDate ?? '').slice(0, 10),
      rentPeriods: occupant.rentPeriods?.length
        ? occupant.rentPeriods.map((p) => ({
            startMonth: monthOf(p.startMonth), endMonth: monthOf(p.endMonth), monthlyRent: p.monthlyRent,
          }))
        : initialRentPeriods(occupant.startDate, occupant.monthlyRent),
    })))
  }, [rental])

  function update(index, patch) {
    setRows((current) => current.map((row, i) => i === index ? { ...row, ...patch, dirty: true } : row))
  }

  async function submit(e) {
    e.preventDefault()
    const changed = rows.filter((row) => row.dirty)
    if (!changed.length) return
    for (const row of changed) {
      const invalid = validateRentPeriods(row.rentPeriods, row.startDate)
      if (invalid) { push(`${row.name}: ${invalid}`, 'error'); return }
    }
    setSaving(true)
    try {
      await updateRental(rental.id, {
        endDate: rental.status === 'ACTIVE' ? null : rental.endDate ? String(rental.endDate).slice(0, 10) : null,
        deposit: rental.deposit ?? null,
        paymentDueDay: rental.paymentDueDay ?? null,
        notes: rental.notes ?? null,
        occupants: changed.map((row) => ({
          id: row.id,
          startDate: row.startDate,
          monthlyRent: Number(row.rentPeriods[row.rentPeriods.length - 1].monthlyRent),
          rentPeriods: rentPeriodsPayload(row.rentPeriods),
          generateMissingPayments: !!row.generateMissingPayments,
        })),
      })
      push('Rent periods saved. Previous months’ payments were preserved.')
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
          Add as many price periods as needed, for example 2018–Jul 2022, Aug 2022–Oct 2025, and Nov 2025–Present.
          The tenant stays in the same rental. Past monthly invoices are never rewritten; only an untouched current-month invoice may be recalculated.
        </p>
        {rows.map((row, index) => (
          <div key={row.id ?? index} className="space-y-3 rounded-xl border border-border p-3">
            <p className="text-sm font-semibold text-ink">{row.name}</p>
            <Field label="Original tenant start date">
              <input type="date" className={inputClass} value={row.startDate}
                onChange={(e) => update(index, {
                  startDate: e.target.value,
                  rentPeriods: [{ ...row.rentPeriods[0], startMonth: monthOf(e.target.value) }, ...row.rentPeriods.slice(1)],
                })} required />
            </Field>
            <RentPeriodsEditor periods={row.rentPeriods} stayStart={row.startDate}
              onChange={(rentPeriods) => update(index, { rentPeriods })} />
            <label className="flex items-start gap-2 text-xs text-ink-soft">
              <input className="mt-0.5" type="checkbox" checked={row.generateMissingPayments}
                onChange={(e) => update(index, { generateMissingPayments: e.target.checked })} />
              <span>Also create missing invoices as UNPAID for earlier months. This can add many overdue amounts.
                Do not select if the past records are not yet verified.</span>
            </label>
          </div>
        ))}
        <div className="flex justify-end gap-2 pt-2">
          <button type="button" className={btnSecondary} onClick={onClose}>Cancel</button>
          <button type="submit" className={btnPrimary} disabled={saving || !rows.some((r) => r.dirty) || rows.some((r) => r.dirty && (r.id == null || validateRentPeriods(r.rentPeriods, r.startDate)))}>
            {saving ? 'Saving…' : 'Save price history'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
