import { useEffect, useMemo, useState } from 'react'
import { listPayments, updatePayment, markPaymentPaid, generateCurrentMonthPayments, deletePayment } from '../services/paymentService'
import { getErrorMessage } from '../services/api'
import { formatCurrency, formatDate, formatMonth, firstOfMonthIso, todayIso } from '../utils/format'
import { useToast } from '../contexts/ToastContext'
import StatusBadge from '../components/StatusBadge'
import EmptyState from '../components/EmptyState'
import ConfirmDialog from '../components/ConfirmDialog'
import Modal from '../components/Modal'
import Field, { inputClass, btnPrimary, btnSecondary } from '../components/Field'

const STATUS_FILTERS = [
  { value: 'ALL', label: 'All' },
  { value: 'PAID', label: 'Paid' },
  { value: 'PARTIALLY_PAID', label: 'Partially paid' },
  { value: 'UNPAID', label: 'Unpaid' },
  { value: 'OVERDUE', label: 'Overdue' },
]

const METHODS = [
  { value: 'CASH', label: 'Cash' },
  { value: 'BANK_TRANSFER', label: 'Bank transfer' },
  { value: 'CHEQUE', label: 'Cheque' },
  { value: 'ONLINE', label: 'Online' },
  { value: 'OTHER', label: 'Other' },
]

export default function Payments() {
  const { push } = useToast()
  const [items, setItems] = useState([])
  const [month, setMonth] = useState(firstOfMonthIso())
  const [year, setYear] = useState(new Date().getFullYear())
  const [status, setStatus] = useState('ALL')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [payTarget, setPayTarget] = useState(null)
  const [payForm, setPayForm] = useState({ amountPaid: '', paymentDate: todayIso(), paymentMethod: 'CASH', notes: '' })
  const [editTarget, setEditTarget] = useState(null)
  const [editForm, setEditForm] = useState({})
  const [saving, setSaving] = useState(false)
  const [generating, setGenerating] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState(null)

  async function load(nextMonth = month, nextYear = year) {
    setLoading(true)
    setError('')
    try {
      const params = {}
      if (nextMonth) params.periodMonth = nextMonth
      if (nextYear) params.year = nextYear
      setItems(await listPayments(params))
    } catch (err) {
      setError(getErrorMessage(err, 'Could not load rent payments.'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const visible = useMemo(() => {
    if (status === 'ALL') return items
    return items.filter((p) => p.effectiveStatus === status || (status !== 'OVERDUE' && p.status === status))
  }, [items, status])

  function openPay(payment) {
    const remaining = Number(payment.amountDue) - Number(payment.amountPaid ?? 0)
    setPayTarget(payment)
    setPayForm({
      amountPaid: remaining > 0 ? remaining : payment.amountDue,
      paymentDate: todayIso(),
      paymentMethod: payment.paymentMethod ?? 'CASH',
      notes: payment.notes ?? '',
    })
  }

  function openEdit(payment) {
    setEditTarget(payment)
    setEditForm({
      amountDue: payment.amountDue ?? '',
      amountPaid: payment.amountPaid ?? 0,
      dueDate: payment.dueDate ?? '',
      paymentDate: payment.paymentDate ?? '',
      paymentMethod: payment.paymentMethod ?? '',
      notes: payment.notes ?? '',
    })
  }

  async function submitPay(e) {
    e.preventDefault()
    setSaving(true)
    try {
      await markPaymentPaid(payTarget.id, {
        amountPaid: Number(payForm.amountPaid),
        paymentDate: payForm.paymentDate || todayIso(),
        paymentMethod: payForm.paymentMethod || null,
        notes: payForm.notes || null,
      })
      push('Payment recorded')
      setPayTarget(null)
      await load()
    } catch (err) {
      push(getErrorMessage(err), 'error')
    } finally {
      setSaving(false)
    }
  }

  async function submitEdit(e) {
    e.preventDefault()
    setSaving(true)
    try {
      await updatePayment(editTarget.id, {
        amountDue: Number(editForm.amountDue),
        amountPaid: editForm.amountPaid === '' ? 0 : Number(editForm.amountPaid),
        dueDate: editForm.dueDate,
        paymentDate: editForm.paymentDate || null,
        paymentMethod: editForm.paymentMethod || null,
        notes: editForm.notes || null,
      })
      push('Payment updated')
      setEditTarget(null)
      await load()
    } catch (err) {
      push(getErrorMessage(err), 'error')
    } finally {
      setSaving(false)
    }
  }

  async function generate() {
    setGenerating(true)
    try {
      const created = await generateCurrentMonthPayments(month)
      push(created.length ? `Created ${created.length} payment record${created.length === 1 ? '' : 's'} for ${formatMonth(month)}` : 'Those payment records are already up to date')
      await load(month)
    } catch (err) {
      push(getErrorMessage(err), 'error')
    } finally {
      setGenerating(false)
    }
  }

  async function confirmDelete() {
    setSaving(true)
    try {
      await deletePayment(deleteTarget.id)
      push('Payment deleted')
      setDeleteTarget(null)
      await load()
    } catch (err) {
      push(getErrorMessage(err), 'error')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="space-y-5">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="font-display text-2xl font-semibold text-ink">Rent Payments</h1>
          <p className="mt-0.5 text-sm text-ink-soft">Record collections without losing past months.</p>
        </div>
        <button type="button" className={btnSecondary} onClick={generate} disabled={generating}>
          {generating ? 'Generating…' : 'Create this month’s rents'}
        </button>
      </div>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <input
          type="month"
          className={`${inputClass} sm:max-w-[11rem]`}
          value={month.slice(0, 7)}
          onChange={(e) => {
            const next = `${e.target.value}-01`
            setMonth(next)
            load(next, year)
          }}
        />
        <input
          type="number"
          min="2000"
          className={`${inputClass} sm:max-w-[8rem]`}
          value={year}
          onChange={(e) => {
            const nextYear = Number(e.target.value || new Date().getFullYear())
            setYear(nextYear)
            load(null, nextYear)
          }}
        />
        <div className="flex flex-wrap gap-2">
          {STATUS_FILTERS.map((filter) => (
            <button
              key={filter.value}
              type="button"
              onClick={() => setStatus(filter.value)}
              className={`rounded-full px-3 py-1.5 text-xs font-medium ${
                status === filter.value ? 'bg-brand text-white' : 'bg-ink/5 text-ink-soft'
              }`}
            >
              {filter.label}
            </button>
          ))}
        </div>
      </div>

      {error && <p className="rounded-lg bg-danger-soft p-3 text-sm text-danger">{error}</p>}

      {loading ? (
        <div className="h-40 animate-pulse rounded-xl border border-border bg-ink/5" />
      ) : visible.length === 0 ? (
        <EmptyState
          title="No payments for this month"
          message="Create this month’s rents after assigning tenants, or pick another month."
        />
      ) : (
        <ul className="space-y-3">
          {visible.map((payment) => {
            const remaining = Number(payment.amountDue) - Number(payment.amountPaid ?? 0)
            return (
              <li key={payment.id} className="rounded-xl border border-border bg-surface p-4 shadow-card">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                  <div>
                    <p className="font-semibold text-ink">
                      {payment.rental?.tenant?.fullName} · {payment.rental?.property?.propertyCode}{' '}
                      {payment.rental?.property?.name}
                    </p>
                    <p className="text-sm text-ink-soft">
                      {formatMonth(payment.periodMonth)} · due {formatDate(payment.dueDate)} ·{' '}
                      {formatCurrency(payment.amountPaid)} of {formatCurrency(payment.amountDue)}
                      {remaining > 0 ? ` · ${formatCurrency(remaining)} left` : ''}
                    </p>
                  </div>
                  <div className="flex flex-wrap items-center gap-2">
                    <StatusBadge status={payment.effectiveStatus} />
                    {payment.effectiveStatus !== 'PAID' && (
                      <button type="button" className={btnPrimary} onClick={() => openPay(payment)}>
                        Record payment
                      </button>
                    )}
                    <button type="button" className={btnSecondary} onClick={() => openEdit(payment)}>
                      Edit
                    </button>
                    <button type="button" className="rounded-lg border border-border px-3 py-2 text-sm font-medium text-danger hover:bg-danger-soft" onClick={() => setDeleteTarget(payment)}>
                      Delete
                    </button>
                  </div>
                </div>
              </li>
            )
          })}
        </ul>
      )}

      <Modal open={!!payTarget} title="Record payment" onClose={() => setPayTarget(null)}>
        {payTarget && (
          <form onSubmit={submitPay} className="space-y-3">
            <p className="text-sm text-ink-soft">
              {payTarget.rental?.tenant?.fullName} · {formatMonth(payTarget.periodMonth)} · due{' '}
              {formatCurrency(payTarget.amountDue)}
            </p>
            <Field label="Amount paid (Rs.)" hint="Enter less than the due amount for a partial payment.">
              <input
                type="number"
                min="0"
                step="0.01"
                className={inputClass}
                value={payForm.amountPaid}
                onChange={(e) => setPayForm({ ...payForm, amountPaid: e.target.value })}
                required
              />
            </Field>
            <Field label="Payment date">
              <input type="date" className={inputClass} value={payForm.paymentDate} onChange={(e) => setPayForm({ ...payForm, paymentDate: e.target.value })} required />
            </Field>
            <Field label="Method">
              <select className={inputClass} value={payForm.paymentMethod} onChange={(e) => setPayForm({ ...payForm, paymentMethod: e.target.value })}>
                {METHODS.map((m) => (
                  <option key={m.value} value={m.value}>{m.label}</option>
                ))}
              </select>
            </Field>
            <Field label="Notes">
              <input className={inputClass} value={payForm.notes} onChange={(e) => setPayForm({ ...payForm, notes: e.target.value })} />
            </Field>
            <div className="flex justify-end gap-2 pt-2">
              <button type="button" className={btnSecondary} onClick={() => setPayTarget(null)}>Cancel</button>
              <button type="submit" className={btnPrimary} disabled={saving}>{saving ? 'Saving…' : 'Save payment'}</button>
            </div>
          </form>
        )}
      </Modal>

      <Modal open={!!editTarget} title="Edit payment" onClose={() => setEditTarget(null)}>
        <form onSubmit={submitEdit} className="space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <Field label="Amount due (Rs.)">
              <input type="number" min="0" step="0.01" className={inputClass} value={editForm.amountDue} onChange={(e) => setEditForm({ ...editForm, amountDue: e.target.value })} required />
            </Field>
            <Field label="Amount paid (Rs.)">
              <input type="number" min="0" step="0.01" className={inputClass} value={editForm.amountPaid} onChange={(e) => setEditForm({ ...editForm, amountPaid: e.target.value })} required />
            </Field>
          </div>
          <Field label="Due date">
            <input type="date" className={inputClass} value={editForm.dueDate} onChange={(e) => setEditForm({ ...editForm, dueDate: e.target.value })} required />
          </Field>
          <Field label="Payment date">
            <input type="date" className={inputClass} value={editForm.paymentDate} onChange={(e) => setEditForm({ ...editForm, paymentDate: e.target.value })} />
          </Field>
          <Field label="Method">
            <select className={inputClass} value={editForm.paymentMethod} onChange={(e) => setEditForm({ ...editForm, paymentMethod: e.target.value })}>
              <option value="">Not set</option>
              {METHODS.map((m) => (
                <option key={m.value} value={m.value}>{m.label}</option>
              ))}
            </select>
          </Field>
          <Field label="Notes">
            <input className={inputClass} value={editForm.notes} onChange={(e) => setEditForm({ ...editForm, notes: e.target.value })} />
          </Field>
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" className={btnSecondary} onClick={() => setEditTarget(null)}>Cancel</button>
            <button type="submit" className={btnPrimary} disabled={saving}>{saving ? 'Saving…' : 'Save'}</button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={!!deleteTarget}
        title="Delete this payment?"
        message="This payment record will be permanently removed from the rental history."
        confirmLabel="Delete"
        danger
        busy={saving}
        onClose={() => setDeleteTarget(null)}
        onConfirm={confirmDelete}
      />
    </div>
  )
}
