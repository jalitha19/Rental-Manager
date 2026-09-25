import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getTenant, updateTenant, addPhone, updatePhone, deletePhone } from '../services/tenantService'
import { tenantRentalHistory, deleteRental, createRental, endRental } from '../services/rentalService'
import { listPayments } from '../services/paymentService'
import { listProperties } from '../services/propertyService'
import { getErrorMessage } from '../services/api'
import { uploadTenantPhoto } from '../services/supabaseStorage'
import { formatDate, formatCurrency, formatMonth, todayIso } from '../utils/format'
import { getOccupants } from '../utils/occupants'
import { useToast } from '../contexts/ToastContext'
import StatusBadge from '../components/StatusBadge'
import Modal from '../components/Modal'
import ConfirmDialog from '../components/ConfirmDialog'
import Field, { inputClass, btnPrimary, btnSecondary } from '../components/Field'

export default function TenantDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { push } = useToast()
  const [tenant, setTenant] = useState(null)
  const [history, setHistory] = useState([])
  const [payments, setPayments] = useState([])
  const [error, setError] = useState('')
  const [editing, setEditing] = useState(false)
  const [form, setForm] = useState({})
  const [saving, setSaving] = useState(false)
  const [phoneOpen, setPhoneOpen] = useState(false)
  const [phoneForm, setPhoneForm] = useState({ phoneNumber: '', label: 'Mobile' })
  const [phoneEdit, setPhoneEdit] = useState(null)
  const [phoneDelete, setPhoneDelete] = useState(null)
  const [historyDelete, setHistoryDelete] = useState(null)
  const [properties, setProperties] = useState([])
  const [historyOpen, setHistoryOpen] = useState(false)
  const [historyForm, setHistoryForm] = useState({
    propertyId: '',
    startDate: todayIso(),
    endDate: '',
    monthlyRent: '',
    deposit: '',
    paymentDueDay: '1',
    notes: '',
  })
  const [printTarget, setPrintTarget] = useState(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (!printTarget) return undefined

    const printTimer = window.setTimeout(() => window.print(), 100)
    const clearPrintTarget = () => setPrintTarget(null)
    window.addEventListener('afterprint', clearPrintTarget)

    return () => {
      window.clearTimeout(printTimer)
      window.removeEventListener('afterprint', clearPrintTarget)
    }
  }, [printTarget])

  async function load() {
    try {
      const [t, rentals, allPayments, propertyList] = await Promise.all([
        getTenant(id),
        tenantRentalHistory(id),
        listPayments(),
        listProperties(),
      ])
      setTenant(t)
      setHistory(rentals)
      setPayments(allPayments.filter((p) => String(p.rental?.tenant?.id) === String(id)))
      setProperties(propertyList)
      setForm({
        tenantType: t.tenantType,
        fullName: t.fullName ?? '',
        nicNumber: t.nicNumber ?? '',
        contactPerson: t.contactPerson ?? '',
        address: t.address ?? '',
        notes: t.notes ?? '',
        photoFile: null,
      })
    } catch (err) {
      setError(getErrorMessage(err, 'Could not load this tenant.'))
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  async function save(e) {
    e.preventDefault()
    setSaving(true)
    try {
      const photoUrl = form.photoFile ? await uploadTenantPhoto(form.photoFile) : tenant.photoUrl
      await updateTenant(id, {
        tenantType: form.tenantType,
        fullName: form.fullName.trim(),
        nicNumber: form.nicNumber.trim() || null,
        contactPerson: form.contactPerson.trim() || null,
        address: form.address.trim() || null,
        notes: form.notes.trim() || null,
        photoUrl,
      })
      push('Tenant updated')
      setEditing(false)
      await load()
    } catch (err) {
      push(getErrorMessage(err), 'error')
    } finally {
      setSaving(false)
    }
  }

  async function savePhone(e) {
    e.preventDefault()
    setSaving(true)
    const payload = { phoneNumber: phoneForm.phoneNumber.trim(), label: phoneForm.label || null }
    try {
      if (phoneEdit) {
        await updatePhone(id, phoneEdit.id, payload)
        push('Phone updated')
      } else {
        await addPhone(id, payload)
        push('Phone added')
      }
      setPhoneOpen(false)
      setPhoneEdit(null)
      await load()
    } catch (err) {
      push(getErrorMessage(err), 'error')
    } finally {
      setSaving(false)
    }
  }

  async function confirmPhoneDelete() {
    setBusy(true)
    try {
      await deletePhone(id, phoneDelete.id)
      push('Phone removed')
      setPhoneDelete(null)
      await load()
    } catch (err) {
      push(getErrorMessage(err), 'error')
    } finally {
      setBusy(false)
    }
  }

  async function addHistoricalRental(e) {
    e.preventDefault()
    setBusy(true)
    try {
      const payload = {
        propertyId: Number(historyForm.propertyId),
        tenantId: Number(id),
        startDate: historyForm.startDate,
        endDate: historyForm.endDate || null,
        monthlyRent: historyForm.monthlyRent === '' ? null : Number(historyForm.monthlyRent),
        deposit: historyForm.deposit === '' ? null : Number(historyForm.deposit),
        paymentDueDay: historyForm.paymentDueDay ? Number(historyForm.paymentDueDay) : null,
        status: 'COMPLETED',
        notes: historyForm.notes || null,
      }

      await createRental(payload)

      push('Previous rental history added')
      setHistoryOpen(false)
      setHistoryForm({
        propertyId: '',
        startDate: todayIso(),
        endDate: '',
        monthlyRent: '',
        deposit: '',
        paymentDueDay: '1',
        notes: '',
      })
      await load()
    } catch (err) {
      push(getErrorMessage(err), 'error')
    } finally {
      setBusy(false)
    }
  }

  function myTerms(rental) {
    const mine = getOccupants(rental).find((o) => String(o.tenant?.id) === String(id))
    return {
      startDate: mine?.startDate ?? rental.startDate,
      endDate: mine?.endDate ?? rental.endDate,
      monthlyRent: mine?.monthlyRent ?? rental.monthlyRent,
    }
  }

  async function confirmHistoryDelete() {
    setBusy(true)
    try {
      await deleteRental(historyDelete.id)
      push('Rental history deleted')
      setHistoryDelete(null)
      await load()
    } catch (err) {
      push(getErrorMessage(err), 'error')
    } finally {
      setBusy(false)
    }
  }

  if (error) {
    return (
      <div>
        <p className="rounded-lg bg-danger-soft p-3 text-sm text-danger">{error}</p>
        <button type="button" className={`${btnSecondary} mt-4`} onClick={() => navigate('/tenants')}>
          Back to tenants
        </button>
      </div>
    )
  }

  if (!tenant) {
    return <div className="h-40 animate-pulse rounded-xl border border-border bg-ink/5" />
  }

  return (
    <div className="space-y-5">
      <Link to="/tenants" className="text-sm text-ink-soft hover:text-ink">
        ← Tenants
      </Link>
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div className="flex items-center gap-4">
          {tenant.photoUrl ? (
            <img src={tenant.photoUrl} alt={`${tenant.fullName} portrait`} className="h-20 w-20 rounded-full object-cover" />
          ) : (
            <div className="flex h-20 w-20 items-center justify-center rounded-full bg-ink/5 text-2xl font-semibold text-ink-soft">
              {tenant.fullName.charAt(0).toUpperCase()}
            </div>
          )}
          <div>
          <h1 className="font-display text-2xl font-semibold text-ink">{tenant.fullName}</h1>
          <p className="text-sm text-ink-soft">
            {tenant.tenantType === 'COMPANY' ? 'Company' : 'Individual'}
            {tenant.nicNumber ? ` · NIC ${tenant.nicNumber}` : ''}
          </p>
          </div>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <button type="button" className={btnSecondary} onClick={() => setHistoryOpen(true)}>
            Add previous history
          </button>
          <button type="button" className={btnSecondary} onClick={() => setPrintTarget({ tenant, history, payments })}>
            Print profile
          </button>
          <button type="button" className={btnSecondary} onClick={() => setEditing(true)}>
            Edit
          </button>
        </div>
      </div>

      <div className="rounded-xl border border-border bg-surface p-5 shadow-card text-sm space-y-1">
        {tenant.contactPerson && <p>Contact person: {tenant.contactPerson}</p>}
        {tenant.address && <p>{tenant.address}</p>}
        {tenant.notes && <p className="text-ink-soft">{tenant.notes}</p>}
      </div>

      <section className="rounded-xl border border-border bg-surface p-5 shadow-card">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="font-display font-semibold text-ink">Phone numbers</h2>
          <button
            type="button"
            className={btnSecondary}
            onClick={() => {
              setPhoneEdit(null)
              setPhoneForm({ phoneNumber: '', label: 'Mobile' })
              setPhoneOpen(true)
            }}
          >
            Add number
          </button>
        </div>
        {tenant.phones?.length ? (
          <ul className="divide-y divide-border">
            {tenant.phones.map((phone) => (
              <li key={phone.id} className="flex items-center justify-between gap-3 py-2 text-sm">
                <span>
                  {phone.phoneNumber}
                  {phone.label ? ` · ${phone.label}` : ''}
                </span>
                <span className="flex gap-2">
                  <button
                    type="button"
                    className="text-ink-soft hover:text-ink"
                    onClick={() => {
                      setPhoneEdit(phone)
                      setPhoneForm({ phoneNumber: phone.phoneNumber, label: phone.label ?? '' })
                      setPhoneOpen(true)
                    }}
                  >
                    Edit
                  </button>
                  <button type="button" className="text-danger" onClick={() => setPhoneDelete(phone)}>
                    Delete
                  </button>
                </span>
              </li>
            ))}
          </ul>
        ) : (
          <p className="text-sm text-ink-soft">No phone numbers yet.</p>
        )}
      </section>

      <section className="rounded-xl border border-border bg-surface p-5 shadow-card">
        <h2 className="mb-3 font-display font-semibold text-ink">Payment history</h2>
        {payments.length === 0 ? (
          <p className="text-sm text-ink-soft">No payments yet. History stays here after a move-out.</p>
        ) : (
          <ul className="space-y-2">
            {payments.map((payment) => (
              <li key={payment.id} className="flex flex-wrap items-center justify-between gap-2 text-sm">
                <span>
                  {formatMonth(payment.periodMonth)} · {payment.rental?.property?.propertyCode} ·{' '}
                  {payment.paymentDate ? `Paid ${formatDate(payment.paymentDate)}` : `Due ${formatDate(payment.dueDate)}`} ·{' '}
                  {formatCurrency(payment.amountPaid)} / {formatCurrency(payment.amountDue)}
                </span>
                <StatusBadge status={payment.effectiveStatus} />
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="rounded-xl border border-border bg-surface p-5 shadow-card">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="font-display font-semibold text-ink">Rental history</h2>
          <button type="button" className={btnSecondary} onClick={() => setHistoryOpen(true)}>
            Add previous history
          </button>
        </div>
        {history.length === 0 ? (
          <p className="text-sm text-ink-soft">No rentals yet.</p>
        ) : (
          <ul className="space-y-2">
            {history.map((rental) => (
              <li key={rental.id} className="flex flex-wrap items-center justify-between gap-2 text-sm">
                <span>
                  {rental.property?.propertyCode} · {rental.property?.name} · {formatDate(myTerms(rental).startDate)} –{' '}
                  {formatDate(myTerms(rental).endDate)} · {formatCurrency(myTerms(rental).monthlyRent)}
                </span>
                <span className="flex items-center gap-3">
                  <StatusBadge status={rental.status} />
                  {rental.status !== 'ACTIVE' && (
                    <button type="button" className="text-danger" onClick={() => setHistoryDelete(rental)}>
                      Delete
                    </button>
                  )}
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>

      <Modal open={editing} title="Edit tenant" onClose={() => setEditing(false)}>
        <form onSubmit={save} className="space-y-3">
          <Field label="Type">
            <select className={inputClass} value={form.tenantType} onChange={(e) => setForm({ ...form, tenantType: e.target.value })}>
              <option value="INDIVIDUAL">Individual</option>
              <option value="COMPANY">Company</option>
            </select>
          </Field>
          <Field label="Name">
            <input className={inputClass} value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} required />
          </Field>
          <Field label="NIC / ID">
            <input className={inputClass} value={form.nicNumber} onChange={(e) => setForm({ ...form, nicNumber: e.target.value })} />
          </Field>
          <Field label="Contact person">
            <input className={inputClass} value={form.contactPerson} onChange={(e) => setForm({ ...form, contactPerson: e.target.value })} />
          </Field>
          <Field label="Address">
            <input className={inputClass} value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} />
          </Field>
          <Field label="Notes">
            <textarea className={inputClass} rows={3} value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} />
          </Field>
          <Field label="Replace photo" hint="Leave empty to keep the current photo. Maximum 5 MB.">
            <input
              type="file"
              accept="image/*"
              className={inputClass}
              onChange={(e) => setForm({ ...form, photoFile: e.target.files?.[0] ?? null })}
            />
          </Field>
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" className={btnSecondary} onClick={() => setEditing(false)}>Cancel</button>
            <button type="submit" className={btnPrimary} disabled={saving}>{saving ? 'Saving…' : 'Save'}</button>
          </div>
        </form>
      </Modal>

      <Modal open={historyOpen} title="Add previous rental history" onClose={() => setHistoryOpen(false)}>
        <form className="space-y-3" onSubmit={addHistoricalRental}>
          <Field label="Property">
            <select className={inputClass} value={historyForm.propertyId} onChange={(e) => setHistoryForm({ ...historyForm, propertyId: e.target.value })} required>
              <option value="">Select a property</option>
              {properties.map((property) => (
                <option key={property.id} value={property.id}>
                  {property.propertyCode} · {property.name}
                </option>
              ))}
            </select>
          </Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Start date">
              <input type="date" className={inputClass} value={historyForm.startDate} onChange={(e) => setHistoryForm({ ...historyForm, startDate: e.target.value })} required />
            </Field>
            <Field label="End date">
              <input type="date" className={inputClass} value={historyForm.endDate} onChange={(e) => setHistoryForm({ ...historyForm, endDate: e.target.value })} />
            </Field>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Monthly rent (Rs.)">
              <input type="number" min="0" step="0.01" className={inputClass} value={historyForm.monthlyRent} onChange={(e) => setHistoryForm({ ...historyForm, monthlyRent: e.target.value })} />
            </Field>
            <Field label="Deposit (Rs.)">
              <input type="number" min="0" step="0.01" className={inputClass} value={historyForm.deposit} onChange={(e) => setHistoryForm({ ...historyForm, deposit: e.target.value })} />
            </Field>
          </div>
          <Field label="Payment due day">
            <input type="number" min="1" max="31" className={inputClass} value={historyForm.paymentDueDay} onChange={(e) => setHistoryForm({ ...historyForm, paymentDueDay: e.target.value })} />
          </Field>
          <Field label="Notes">
            <textarea className={inputClass} rows={2} value={historyForm.notes} onChange={(e) => setHistoryForm({ ...historyForm, notes: e.target.value })} />
          </Field>
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" className={btnSecondary} onClick={() => setHistoryOpen(false)}>Cancel</button>
            <button type="submit" className={btnPrimary} disabled={busy}>{busy ? 'Saving…' : 'Add history'}</button>
          </div>
        </form>
      </Modal>

      <Modal open={phoneOpen} title={phoneEdit ? 'Edit phone' : 'Add phone'} onClose={() => setPhoneOpen(false)}>
        <form onSubmit={savePhone} className="space-y-3">
          <Field label="Phone number">
            <input className={inputClass} value={phoneForm.phoneNumber} onChange={(e) => setPhoneForm({ ...phoneForm, phoneNumber: e.target.value })} required />
          </Field>
          <Field label="Label">
            <input className={inputClass} value={phoneForm.label} onChange={(e) => setPhoneForm({ ...phoneForm, label: e.target.value })} placeholder="Mobile, Work…" />
          </Field>
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" className={btnSecondary} onClick={() => setPhoneOpen(false)}>Cancel</button>
            <button type="submit" className={btnPrimary} disabled={saving}>{saving ? 'Saving…' : 'Save'}</button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={!!phoneDelete}
        title="Delete this number?"
        message="The tenant stays. Only this phone number is removed."
        confirmLabel="Delete"
        danger
        busy={busy}
        onClose={() => setPhoneDelete(null)}
        onConfirm={confirmPhoneDelete}
      />

      <ConfirmDialog
        open={!!historyDelete}
        title="Delete this rental history?"
        message="This removes the rental and all rent payment records attached to it. This cannot be undone."
        confirmLabel="Delete history"
        danger
        busy={busy}
        onClose={() => setHistoryDelete(null)}
        onConfirm={confirmHistoryDelete}
      />

      {printTarget && (
        <article className="print-sheet" aria-label="Tenant profile printout">
          <header className="print-sheet-header">
            <div>
              <p className="print-sheet-kicker">Tenant profile</p>
              <h1>{printTarget.tenant.fullName}</h1>
            </div>
            <p className="print-sheet-reference">Tenant #{printTarget.tenant.id}</p>
          </header>

          <section className="print-sheet-section">
            <h2>Tenant details</h2>
            <div className="print-grid">
              <div><span>Type</span><strong>{printTarget.tenant.tenantType === 'COMPANY' ? 'Company' : 'Individual'}</strong></div>
              <div><span>NIC / ID</span><strong>{printTarget.tenant.nicNumber || '—'}</strong></div>
              <div><span>Contact person</span><strong>{printTarget.tenant.contactPerson || '—'}</strong></div>
              <div className="print-grid-wide"><span>Address</span><strong>{printTarget.tenant.address || '—'}</strong></div>
            </div>
          </section>

          <section className="print-sheet-section">
            <h2>Rental history</h2>
            {printTarget.history.length === 0 ? (
              <p className="print-sheet-notes">No rental records.</p>
            ) : (
              <div className="space-y-2">
                {printTarget.history.map((rental) => (
                  <div key={rental.id} className="print-history-row">
                    <span><strong>{rental.property?.propertyCode || '—'}</strong> · {rental.property?.name || '—'}</span>
                    <span>{formatDate(myTerms(rental).startDate)} – {formatDate(myTerms(rental).endDate)}</span>
                    <span>{formatCurrency(myTerms(rental).monthlyRent)}</span>
                    <span>{rental.status}</span>
                  </div>
                ))}
              </div>
            )}
          </section>

          <section className="print-sheet-section">
            <h2>Payment history</h2>
            {printTarget.payments.length === 0 ? (
              <p className="print-sheet-notes">No payment records.</p>
            ) : (
              <div className="space-y-2">
                {printTarget.payments.map((payment) => (
                  <div key={payment.id} className="print-history-row">
                    <span>{formatMonth(payment.periodMonth)} · {payment.rental?.property?.propertyCode || '—'}</span>
                    <span>{payment.paymentDate ? `Paid ${formatDate(payment.paymentDate)}` : `Due ${formatDate(payment.dueDate)}`}</span>
                    <span>{formatCurrency(payment.amountPaid)} / {formatCurrency(payment.amountDue)}</span>
                    <span>{payment.effectiveStatus}</span>
                  </div>
                ))}
              </div>
            )}
          </section>

          <footer className="print-sheet-footer">
            <span>Printed {formatDate(new Date().toISOString())}</span>
            <span>Rental Manager</span>
          </footer>
        </article>
      )}
    </div>
  )
}
