import { useEffect, useState } from 'react'
import { updateRental } from '../services/rentalService'
import { getErrorMessage } from '../services/api'
import { getOccupants } from '../utils/occupants'
import { useToast } from '../contexts/ToastContext'
import Modal from './Modal'
import Field, { inputClass, btnPrimary, btnSecondary } from './Field'

export default function OccupantTermsModal({ rental, onClose, onSaved }) {
  const { push } = useToast()
  const [rows, setRows] = useState([])
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (!rental) {
      setRows([])
      return
    }
    setRows(
      getOccupants(rental).map((occupant) => ({
        id: occupant.id,
        name: occupant.tenant?.fullName ?? '',
        startDate: String(occupant.startDate ?? '').slice(0, 10),
        monthlyRent: occupant.monthlyRent ?? '',
      }))
    )
  }, [rental])

  function update(index, patch) {
    setRows((current) => current.map((row, i) => (i === index ? { ...row, ...patch } : row)))
  }

  async function submit(e) {
    e.preventDefault()
    setSaving(true)
    try {
      await updateRental(rental.id, {
        endDate: rental.status === 'ACTIVE' ? null : rental.endDate ? String(rental.endDate).slice(0, 10) : null,
        deposit: rental.deposit ?? null,
        paymentDueDay: rental.paymentDueDay ?? null,
        notes: rental.notes ?? null,
        occupants: rows.map((row) => ({
          id: row.id,
          startDate: row.startDate,
          monthlyRent: Number(row.monthlyRent),
        })),
      })
      push('Tenant terms updated')
      onClose()
      if (onSaved) await onSaved()
    } catch (err) {
      push(getErrorMessage(err), 'error')
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal open={!!rental} title="Edit tenant terms" onClose={onClose}>
      <form onSubmit={submit} className="space-y-3">
        <p className="text-sm text-ink-soft">
          Set each tenant's own start date and monthly rent. Existing payment records are not changed. Missing months
          are added the next time rents are created, and you can edit an existing month on the Payments page.
        </p>
        {rows.map((row, index) => (
          <div key={row.id ?? index} className="space-y-3 rounded-lg border border-border p-3">
            <p className="text-sm font-semibold text-ink">{row.name}</p>
            <div className="grid grid-cols-2 gap-3">
              <Field label="Start date">
                <input
                  type="date"
                  className={inputClass}
                  value={row.startDate}
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
                  value={row.monthlyRent}
                  onChange={(e) => update(index, { monthlyRent: e.target.value })}
                  required
                />
              </Field>
            </div>
          </div>
        ))}
        <div className="flex justify-end gap-2 pt-2">
          <button type="button" className={btnSecondary} onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className={btnPrimary} disabled={saving || rows.some((r) => r.id == null)}>
            {saving ? 'Saving…' : 'Save'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
