import { useEffect, useState } from 'react'
import { listRentals, createRental, endRental, changeTenant, deleteRental } from '../services/rentalService'
import { listPayments } from '../services/paymentService'
import { listProperties } from '../services/propertyService'
import { listTenants } from '../services/tenantService'
import { getErrorMessage } from '../services/api'
import { formatCurrency, formatDate, todayIso } from '../utils/format'
import { useToast } from '../contexts/ToastContext'
import StatusBadge from '../components/StatusBadge'
import EmptyState from '../components/EmptyState'
import Modal from '../components/Modal'
import ConfirmDialog from '../components/ConfirmDialog'
import Field, { inputClass, btnPrimary, btnSecondary } from '../components/Field'

export default function Rentals() {
  const { push } = useToast()
  const [items, setItems] = useState([])
  const [status, setStatus] = useState('ACTIVE')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [properties, setProperties] = useState([])
  const [tenants, setTenants] = useState([])
  const [createOpen, setCreateOpen] = useState(false)
  const [form, setForm] = useState({
    propertyId: '',
    tenantId: '',
    startDate: todayIso(),
    monthlyRent: '',
    deposit: '',
    paymentDueDay: '1',
    notes: '',
  })
  const [saving, setSaving] = useState(false)
  const [endTarget, setEndTarget] = useState(null)
  const [endForm, setEndForm] = useState({ endDate: todayIso(), status: 'COMPLETED', notes: '' })
  const [changeTarget, setChangeTarget] = useState(null)
  const [changeForm, setChangeForm] = useState({
    newTenantId: '',
    newStartDate: todayIso(),
    monthlyRent: '',
    paymentDueDay: '1',
  })
  const [busy, setBusy] = useState(false)
  const [occupiedIds, setOccupiedIds] = useState(() => new Set())
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [printTarget, setPrintTarget] = useState(null)
  const [monthlyPrintTarget, setMonthlyPrintTarget] = useState(null)
  const [printMonth, setPrintMonth] = useState(todayIso().slice(0, 7))

  useEffect(() => {
    if (!printTarget && !monthlyPrintTarget) return undefined

    const printTimer = window.setTimeout(() => window.print(), 100)
    const clearPrintTarget = () => setPrintTarget(null)
    window.addEventListener('afterprint', clearPrintTarget)

    return () => {
      window.clearTimeout(printTimer)
      window.removeEventListener('afterprint', clearPrintTarget)
    }
  }, [printTarget, monthlyPrintTarget])

  async function refreshLookups() {
    try {
      const [p, t, active] = await Promise.all([
        listProperties(),
        listTenants(),
        listRentals({ status: 'ACTIVE' }),
      ])
      setProperties(p)
      setTenants(t)
      setOccupiedIds(new Set(active.map((r) => r.property?.id).filter(Boolean)))
    } catch {
      // List endpoints already surface errors on the main load.
    }
  }

  async function load(nextStatus = status) {
    setLoading(true)
    setError('')
    try {
      const params = nextStatus === 'ALL' ? {} : { status: nextStatus }
      setItems(await listRentals(params))
    } catch (err) {
      setError(getErrorMessage(err, 'Could not load rentals.'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    refreshLookups()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function onPropertyChange(propertyId) {
    const property = properties.find((p) => String(p.id) === String(propertyId))
    setForm((f) => ({
      ...f,
      propertyId,
      monthlyRent: property?.monthlyRent ?? f.monthlyRent,
      deposit: property?.deposit ?? f.deposit,
    }))
  }

  async function save(e) {
    e.preventDefault()
    setSaving(true)
    try {
      await createRental({
        propertyId: Number(form.propertyId),
        tenantId: Number(form.tenantId),
        startDate: form.startDate,
        monthlyRent: form.monthlyRent === '' ? null : Number(form.monthlyRent),
        deposit: form.deposit === '' ? null : Number(form.deposit),
        paymentDueDay: form.paymentDueDay ? Number(form.paymentDueDay) : null,
        notes: form.notes || null,
      })
      push('Rental created')
      setCreateOpen(false)
      await Promise.all([load(), refreshLookups()])
    } catch (err) {
      push(getErrorMessage(err), 'error')
    } finally {
      setSaving(false)
    }
  }

  async function confirmEnd() {
    setBusy(true)
    try {
      await endRental(endTarget.id, endForm)
      push('Rental ended')
      setEndTarget(null)
      await Promise.all([load(), refreshLookups()])
    } catch (err) {
      push(getErrorMessage(err), 'error')
    } finally {
      setBusy(false)
    }
  }

  async function submitChange(e) {
    e.preventDefault()
    setSaving(true)
    try {
      await changeTenant(changeTarget.property.id, {
        newTenantId: Number(changeForm.newTenantId),
        newStartDate: changeForm.newStartDate,
        monthlyRent: changeForm.monthlyRent === '' ? null : Number(changeForm.monthlyRent),
        paymentDueDay: changeForm.paymentDueDay ? Number(changeForm.paymentDueDay) : null,
      })
      push('Tenant changed. Old rental kept in history.')
      setChangeTarget(null)
      await Promise.all([load(), refreshLookups()])
    } catch (err) {
      push(getErrorMessage(err), 'error')
    } finally {
      setSaving(false)
    }
  }

  async function confirmDelete() {
    setBusy(true)
    try {
      await deleteRental(deleteTarget.id)
      push('Rental history deleted')
      setDeleteTarget(null)
      await load()
    } catch (err) {
      push(getErrorMessage(err), 'error')
    } finally {
      setBusy(false)
    }
  }

  async function printMonthlyReport() {
    try {
      const [allRentals, monthlyPayments] = await Promise.all([
        listRentals({}),
        listPayments({ periodMonth: `${printMonth}-01` }),
      ])
      const monthStart = `${printMonth}-01`
      const monthEnd = `${printMonth}-${new Date(Number(printMonth.slice(0, 4)), Number(printMonth.slice(5, 7)), 0).getDate()}`
      const rentals = allRentals.filter((rental) => {
        const startDate = String(rental.startDate || '').slice(0, 10)
        const endDate = rental.endDate ? String(rental.endDate).slice(0, 10) : null
        return startDate <= monthEnd && (!endDate || endDate >= monthStart)
      })
      const paymentsByRentalId = new Map(monthlyPayments.map((payment) => [String(payment.rental?.id), payment]))
      setMonthlyPrintTarget({ month: printMonth, rentals, paymentsByRentalId })
    } catch (err) {
      push(getErrorMessage(err, 'Could not prepare the monthly report.'), 'error')
    }
  }

  return (
    <>
      <div className="print:hidden space-y-5">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="font-display text-2xl font-semibold text-ink">Rentals</h1>
          <p className="mt-0.5 text-sm text-ink-soft">Assign tenants without overwriting past occupancy.</p>
        </div>
        <div className="flex flex-wrap items-end gap-2">
          <label className="text-xs font-medium text-ink-soft">
            Month
            <input type="month" value={printMonth} onChange={(e) => setPrintMonth(e.target.value)} className={`${inputClass} mt-1`} />
          </label>
          <button type="button" className={btnSecondary} onClick={printMonthlyReport}>
            Print month
          </button>
          <button type="button" className={btnPrimary} onClick={() => setCreateOpen(true)}>
            New rental
          </button>
        </div>
      </div>

      <div className="flex flex-wrap gap-2">
        {['ACTIVE', 'COMPLETED', 'CANCELLED', 'ALL'].map((value) => (
          <button
            key={value}
            type="button"
            onClick={() => {
              setStatus(value)
              load(value)
            }}
            className={`rounded-full px-3 py-1.5 text-xs font-medium ${
              status === value ? 'bg-brand text-white' : 'bg-ink/5 text-ink-soft'
            }`}
          >
            {value === 'ALL' ? 'All history' : value.charAt(0) + value.slice(1).toLowerCase()}
          </button>
        ))}
      </div>

      {error && <p className="rounded-lg bg-danger-soft p-3 text-sm text-danger">{error}</p>}

      {loading ? (
        <div className="h-40 animate-pulse rounded-xl border border-border bg-ink/5" />
      ) : items.length === 0 ? (
        <EmptyState title="No rentals" message="Create a rental to assign a tenant to a house or room." />
      ) : (
        <ul className="space-y-3">
          {items.map((rental) => (
            <li key={rental.id} className="rounded-xl border border-border bg-surface p-4 shadow-card">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div>
                  <p className="font-semibold text-ink">
                    {rental.property?.propertyCode} · {rental.property?.name}
                  </p>
                  <p className="text-sm text-ink-soft">
                    {rental.tenant?.fullName} · {formatDate(rental.startDate)} – {formatDate(rental.endDate)} ·{' '}
                    {formatCurrency(rental.monthlyRent)}
                  </p>
                </div>
                <div className="flex flex-wrap items-center gap-2">
                  <StatusBadge status={rental.status} />
                  <button type="button" className={btnSecondary} onClick={() => setPrintTarget(rental)}>
                    Print details
                  </button>
                  {rental.status === 'ACTIVE' && (
                    <>
                      <button
                        type="button"
                        className={btnSecondary}
                        onClick={() => {
                          setChangeTarget(rental)
                          setChangeForm({
                            newTenantId: '',
                            newStartDate: todayIso(),
                            monthlyRent: rental.monthlyRent ?? '',
                            paymentDueDay: rental.paymentDueDay ?? 1,
                          })
                        }}
                      >
                        Change tenant
                      </button>
                      <button
                        type="button"
                        className="rounded-lg border border-border px-4 py-2.5 text-sm font-medium text-danger hover:bg-danger-soft"
                        onClick={() => {
                          setEndTarget(rental)
                          setEndForm({ endDate: todayIso(), status: 'COMPLETED', notes: '' })
                        }}
                      >
                        End rental
                      </button>
                    </>
                  )}
                  {rental.status !== 'ACTIVE' && (
                    <button type="button" className="rounded-lg border border-border px-4 py-2.5 text-sm font-medium text-danger hover:bg-danger-soft" onClick={() => setDeleteTarget(rental)}>
                      Delete history
                    </button>
                  )}
                </div>
              </div>
            </li>
          ))}
        </ul>
      )}

      <Modal open={createOpen} title="New rental" onClose={() => setCreateOpen(false)}>
        <form onSubmit={save} className="space-y-3">
          <Field label="Property">
            <select className={inputClass} value={form.propertyId} onChange={(e) => onPropertyChange(e.target.value)} required>
              <option value="">Select house or room</option>
              {properties.map((p) => (
                <option key={p.id} value={p.id} disabled={p.status === 'OCCUPIED' || occupiedIds.has(p.id)}>
                  {p.propertyCode} · {p.name} ({p.status === 'OCCUPIED' || occupiedIds.has(p.id) ? 'Occupied — use Change tenant' : p.status})
                </option>
              ))}
            </select>
          </Field>
          <Field label="Tenant">
            <select className={inputClass} value={form.tenantId} onChange={(e) => setForm({ ...form, tenantId: e.target.value })} required>
              <option value="">Select tenant</option>
              {tenants.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.fullName}
                </option>
              ))}
            </select>
          </Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Monthly rent (Rs.)">
              <input type="number" min="0" step="0.01" className={inputClass} value={form.monthlyRent} onChange={(e) => setForm({ ...form, monthlyRent: e.target.value })} />
            </Field>
            <Field label="Due day">
              <input type="number" min="1" max="31" className={inputClass} value={form.paymentDueDay} onChange={(e) => setForm({ ...form, paymentDueDay: e.target.value })} />
            </Field>
          </div>
          <Field label="Start date">
            <input type="date" className={inputClass} value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} required />
          </Field>
          <p className="text-xs text-ink-faint">A property cannot have two active rentals. Use Change tenant if it is already occupied.</p>
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" className={btnSecondary} onClick={() => setCreateOpen(false)}>Cancel</button>
            <button type="submit" className={btnPrimary} disabled={saving}>{saving ? 'Saving…' : 'Create'}</button>
          </div>
        </form>
      </Modal>

      <Modal open={!!changeTarget} title="Change tenant" onClose={() => setChangeTarget(null)}>
        <p className="mb-4 text-sm text-ink-soft">Ends the current occupancy and starts a new one. History is kept.</p>
        <form onSubmit={submitChange} className="space-y-3">
          <Field label="New tenant">
            <select className={inputClass} value={changeForm.newTenantId} onChange={(e) => setChangeForm({ ...changeForm, newTenantId: e.target.value })} required>
              <option value="">Select tenant</option>
              {tenants.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.fullName}
                </option>
              ))}
            </select>
          </Field>
          <Field label="New start date">
            <input type="date" className={inputClass} value={changeForm.newStartDate} onChange={(e) => setChangeForm({ ...changeForm, newStartDate: e.target.value })} required />
          </Field>
          <Field label="Monthly rent (Rs.)">
            <input type="number" min="0" step="0.01" className={inputClass} value={changeForm.monthlyRent} onChange={(e) => setChangeForm({ ...changeForm, monthlyRent: e.target.value })} />
          </Field>
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" className={btnSecondary} onClick={() => setChangeTarget(null)}>Cancel</button>
            <button type="submit" className={btnPrimary} disabled={saving}>{saving ? 'Saving…' : 'Change tenant'}</button>
          </div>
        </form>
      </Modal>

      <Modal open={!!endTarget} title="End rental" onClose={() => setEndTarget(null)}>
        <form
          className="space-y-3"
          onSubmit={(e) => {
            e.preventDefault()
            confirmEnd()
          }}
        >
          <Field label="End date">
            <input type="date" className={inputClass} value={endForm.endDate} onChange={(e) => setEndForm({ ...endForm, endDate: e.target.value })} required />
          </Field>
          <Field label="Reason">
            <select className={inputClass} value={endForm.status} onChange={(e) => setEndForm({ ...endForm, status: e.target.value })}>
              <option value="COMPLETED">Move-out completed</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
          </Field>
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" className={btnSecondary} onClick={() => setEndTarget(null)}>Cancel</button>
            <button type="submit" className={btnPrimary} disabled={busy}>{busy ? 'Saving…' : 'End rental'}</button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={!!deleteTarget}
        title="Delete rental history?"
        message="This removes the rental and all payment records attached to it. This cannot be undone."
        confirmLabel="Delete history"
        danger
        busy={busy}
        onClose={() => setDeleteTarget(null)}
        onConfirm={confirmDelete}
      />
      </div>

      {printTarget && (
        <article className="print-sheet" aria-label="Rental details printout">
          <header className="print-sheet-header">
            <div>
              <p className="print-sheet-kicker">Rental agreement details</p>
              <h1>{printTarget.property?.name || printTarget.property?.propertyCode || 'Rental'}</h1>
            </div>
            <p className="print-sheet-reference">Rental #{printTarget.id}</p>
          </header>

          <section className="print-sheet-section">
            <h2>Property</h2>
            <div className="print-grid">
              <div><span>Property code</span><strong>{printTarget.property?.propertyCode || '—'}</strong></div>
              <div><span>Type</span><strong>{printTarget.property?.type || '—'}</strong></div>
              <div className="print-grid-wide"><span>Address</span><strong>{printTarget.property?.address || '—'}</strong></div>
            </div>
          </section>

          <section className="print-sheet-section">
            <h2>Occupancy</h2>
            <div className="print-grid">
              <div><span>Tenant</span><strong>{printTarget.tenant?.fullName || '—'}</strong></div>
              {printTarget.tenant2 && <div><span>Additional tenant</span><strong>{printTarget.tenant2.fullName}</strong></div>}
              <div><span>Status</span><strong>{printTarget.status || '—'}</strong></div>
              <div><span>Start date</span><strong>{formatDate(printTarget.startDate)}</strong></div>
              <div><span>End date</span><strong>{formatDate(printTarget.endDate)}</strong></div>
            </div>
          </section>

          <section className="print-sheet-section">
            <h2>Financial terms</h2>
            <div className="print-grid">
              <div><span>Monthly rent</span><strong>{formatCurrency(printTarget.monthlyRent)}</strong></div>
              <div><span>Deposit</span><strong>{formatCurrency(printTarget.deposit)}</strong></div>
              <div><span>Payment due day</span><strong>{printTarget.paymentDueDay ? `Day ${printTarget.paymentDueDay} of each month` : '—'}</strong></div>
            </div>
          </section>

          {printTarget.notes && (
            <section className="print-sheet-section">
              <h2>Notes</h2>
              <p className="print-sheet-notes">{printTarget.notes}</p>
            </section>
          )}

          <footer className="print-sheet-footer">
            <span>Printed {formatDate(new Date().toISOString())}</span>
            <span>Rental Manager</span>
          </footer>
        </article>
      )}

      {monthlyPrintTarget && (
        <article className="print-sheet monthly-print-sheet" aria-label="Monthly rental report">
          <header className="print-sheet-header">
            <div>
              <p className="print-sheet-kicker">Monthly rental report</p>
              <h1>{new Date(`${monthlyPrintTarget.month}-01T00:00:00`).toLocaleDateString('en-GB', { month: 'long', year: 'numeric' })}</h1>
            </div>
            <p className="print-sheet-reference">{monthlyPrintTarget.rentals.length} rental{monthlyPrintTarget.rentals.length === 1 ? '' : 's'}</p>
          </header>

          <table className="monthly-print-table">
            <thead>
              <tr>
                <th>Property</th>
                <th>Tenant</th>
                <th>Period</th>
                <th>Status</th>
                <th>Payment</th>
                <th className="monthly-print-number">Monthly rent</th>
              </tr>
            </thead>
            <tbody>
              {monthlyPrintTarget.rentals.map((rental) => (
                <tr key={rental.id}>
                  {(() => {
                    const payment = monthlyPrintTarget.paymentsByRentalId.get(String(rental.id))
                    const amountPaid = Number(payment?.amountPaid ?? 0)
                    const amountDue = Number(payment?.amountDue ?? rental.monthlyRent ?? 0)
                    const balance = Math.max(amountDue - amountPaid, 0)
                    return (
                      <>
                  <td><strong>{rental.property?.propertyCode || '—'}</strong><span>{rental.property?.name || '—'}</span></td>
                  <td><strong>{rental.tenant?.fullName || '—'}</strong>{rental.tenant2 && <span>{rental.tenant2.fullName}</span>}</td>
                  <td>{formatDate(rental.startDate)} – {formatDate(rental.endDate)}</td>
                  <td>{rental.status || '—'}</td>
                  <td><strong>{payment?.effectiveStatus || 'NOT GENERATED'}</strong><span>{formatCurrency(amountPaid)} paid · {formatCurrency(balance)} due</span></td>
                  <td className="monthly-print-number">{formatCurrency(rental.monthlyRent)}</td>
                      </>
                    )
                  })()}
                </tr>
              ))}
              {monthlyPrintTarget.rentals.length === 0 && (
                <tr><td colSpan="6" className="monthly-print-empty">No rentals were active during this month.</td></tr>
              )}
            </tbody>
          </table>

          <footer className="print-sheet-footer">
            <span>Deposit and due-day details are available in individual rental printouts.</span>
            <span>Rental Manager</span>
          </footer>
        </article>
      )}
    </>
  )
}
