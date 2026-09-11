import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { listProperties, searchProperties, createProperty, updateProperty, deleteProperty } from '../services/propertyService'
import { listRentals, propertyRentalHistory, changeTenant } from '../services/rentalService'
import { listTenants } from '../services/tenantService'
import { getErrorMessage } from '../services/api'
import { formatCurrency, formatDate, todayIso } from '../utils/format'
import { useToast } from '../contexts/ToastContext'
import StatusBadge from '../components/StatusBadge'
import EmptyState from '../components/EmptyState'
import Modal from '../components/Modal'
import ConfirmDialog from '../components/ConfirmDialog'
import Field, { inputClass, btnPrimary, btnSecondary } from '../components/Field'

const EMPTY_FORM = {
  propertyCode: '',
  name: '',
  address: '',
  monthlyRent: '',
  deposit: '',
  status: 'AVAILABLE',
  notes: '',
}

export default function PropertiesPage({ type, title }) {
  const { push } = useToast()
  const [searchParams] = useSearchParams()
  const [items, setItems] = useState([])
  const [activeRentals, setActiveRentals] = useState([])
  const [query, setQuery] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [saving, setSaving] = useState(false)
  const [detail, setDetail] = useState(null)
  const [history, setHistory] = useState([])
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [busy, setBusy] = useState(false)
  const [tenants, setTenants] = useState([])
  const [changeOpen, setChangeOpen] = useState(null)
  const [changeForm, setChangeForm] = useState({
    newTenantId: '',
    newTenant2Id: '',
    newStartDate: todayIso(),
    monthlyRent: '',
    deposit: '',
    paymentDueDay: '1',
    notes: '',
  })

  async function load(search = query) {
    setLoading(true)
    setError('')
    try {
      const q = (search ?? '').trim()
      const [properties, rentals] = await Promise.all([
        q ? searchProperties(q) : listProperties(type),
        listRentals({ status: 'ACTIVE' }),
      ])
      const filtered = q ? properties.filter((p) => p.type === type) : properties
      setItems(filtered)
      setActiveRentals(rentals)
    } catch (err) {
      setError(getErrorMessage(err, `Could not load ${title.toLowerCase()}.`))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    const q = searchParams.get('q') ?? ''
    setQuery(q)
    load(q)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [type, searchParams])

  const occupancy = useMemo(() => {
    const map = new Map()
    for (const rental of activeRentals) {
      map.set(rental.property?.id, rental)
    }
    return map
  }, [activeRentals])

  function openCreate() {
    setEditing(null)
    setForm(EMPTY_FORM)
    setFormOpen(true)
  }

  function openEdit(property) {
    setEditing(property)
    setForm({
      propertyCode: property.propertyCode ?? '',
      name: property.name ?? '',
      address: property.address ?? '',
      monthlyRent: property.monthlyRent ?? '',
      deposit: property.deposit ?? '',
      status: property.status ?? 'AVAILABLE',
      notes: property.notes ?? '',
    })
    setFormOpen(true)
  }

  async function save(e) {
    e.preventDefault()
    setSaving(true)
    const payload = {
      propertyCode: form.propertyCode.trim(),
      type,
      name: form.name.trim(),
      address: form.address.trim(),
      monthlyRent: Number(form.monthlyRent),
      deposit: form.deposit === '' ? null : Number(form.deposit),
      status: form.status,
      notes: form.notes || null,
    }
    try {
      if (editing) {
        await updateProperty(editing.id, payload)
        push(`${title.slice(0, -1)} updated`)
      } else {
        await createProperty(payload)
        push(`${title.slice(0, -1)} added`)
      }
      setFormOpen(false)
      await load()
    } catch (err) {
      push(getErrorMessage(err), 'error')
    } finally {
      setSaving(false)
    }
  }

  async function openDetail(property) {
    setDetail(property)
    try {
      setHistory(await propertyRentalHistory(property.id))
    } catch {
      setHistory([])
    }
  }

  async function confirmDelete() {
    setBusy(true)
    try {
      await deleteProperty(deleteTarget.id)
      push('Property deleted')
      setDeleteTarget(null)
      await load()
    } catch (err) {
      push(getErrorMessage(err), 'error')
    } finally {
      setBusy(false)
    }
  }

  async function openChange(property) {
    if (tenants.length === 0) {
      try {
        setTenants(await listTenants())
      } catch (err) {
        push(getErrorMessage(err, 'Could not load tenants.'), 'error')
        return
      }
    }
    const rental = occupancy.get(property.id)
    setChangeOpen(property)
    setChangeForm({
      newTenantId: '',
      newTenant2Id: '',
      newStartDate: todayIso(),
      monthlyRent: rental?.monthlyRent ?? property.monthlyRent ?? '',
      deposit: rental?.deposit ?? property.deposit ?? '',
      paymentDueDay: rental?.paymentDueDay ?? 1,
      notes: '',
    })
  }

  async function submitChange(e) {
    e.preventDefault()
    setSaving(true)
    try {
      await changeTenant(changeOpen.id, {
        newTenantId: Number(changeForm.newTenantId),
        newTenant2Id: changeForm.newTenant2Id ? Number(changeForm.newTenant2Id) : null,
        newStartDate: changeForm.newStartDate,
        monthlyRent: changeForm.monthlyRent === '' ? null : Number(changeForm.monthlyRent),
        deposit: changeForm.deposit === '' ? null : Number(changeForm.deposit),
        paymentDueDay: changeForm.paymentDueDay ? Number(changeForm.paymentDueDay) : null,
        notes: changeForm.notes || null,
      })
      push('Tenant changed. Previous rental was kept in history.')
      setChangeOpen(null)
      setDetail(null)
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
          <h1 className="font-display text-2xl font-semibold text-ink">{title}</h1>
          <p className="mt-0.5 text-sm text-ink-soft">{items.length} recorded</p>
        </div>
        <button type="button" className={btnPrimary} onClick={openCreate}>
          Add {type === 'HOUSE' ? 'house' : 'room'}
        </button>
      </div>

      <form
        onSubmit={(e) => {
          e.preventDefault()
          load()
        }}
        className="flex gap-2"
      >
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder={`Search ${title.toLowerCase()} by code, name, or address`}
          className={inputClass}
        />
        <button type="submit" className={btnSecondary}>
          Search
        </button>
      </form>

      {error && <p className="rounded-lg bg-danger-soft p-3 text-sm text-danger">{error}</p>}

      {loading ? (
        <div className="space-y-3 animate-pulse">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="h-24 rounded-xl border border-border bg-ink/5" />
          ))}
        </div>
      ) : items.length === 0 ? (
        <EmptyState
          title={`No ${title.toLowerCase()} yet`}
          message="Add the first one to start tracking occupancy and rent."
          action={
            <button type="button" className={btnPrimary} onClick={openCreate}>
              Add {type === 'HOUSE' ? 'house' : 'room'}
            </button>
          }
        />
      ) : (
        <ul className="space-y-3">
          {items.map((property) => {
            const rental = occupancy.get(property.id)
            const tenantNames = rental
              ? [rental.tenant?.fullName, rental.tenant2?.fullName].filter(Boolean).join(' & ')
              : 'No current tenant'
            
            return (
              <li key={property.id} className="rounded-xl border border-border bg-surface p-4 shadow-card">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                  <button type="button" className="text-left" onClick={() => openDetail(property)}>
                    <p className="text-sm font-semibold text-ink">
                      {property.propertyCode} · {property.name}
                    </p>
                    <p className="mt-0.5 text-xs text-ink-soft">{property.address}</p>
                    <p className="mt-2 text-sm text-ink">
                      Rent {formatCurrency(property.monthlyRent)} · {tenantNames}
                    </p>
                  </button>
                  <div className="flex flex-wrap items-center gap-2">
                    <StatusBadge status={property.status} />
                    <button type="button" className={btnSecondary} onClick={() => openEdit(property)}>
                      Edit
                    </button>
                    <button type="button" className={btnSecondary} onClick={() => openChange(property)}>
                      {rental ? 'Change tenant' : 'Assign tenant'}
                    </button>
                    <button
                      type="button"
                      className="rounded-lg border border-border px-4 py-2.5 text-sm font-medium text-danger hover:bg-danger-soft"
                      onClick={() => setDeleteTarget(property)}
                    >
                      Delete
                    </button>
                  </div>
                </div>
              </li>
            )
          })}
        </ul>
      )}

      <Modal
        open={formOpen}
        title={editing ? `Edit ${type === 'HOUSE' ? 'house' : 'room'}` : `Add ${type === 'HOUSE' ? 'house' : 'room'}`}
        onClose={() => setFormOpen(false)}
      >
        <form onSubmit={save} className="space-y-3">
          <Field label="Code">
            <input className={inputClass} value={form.propertyCode} onChange={(e) => setForm({ ...form, propertyCode: e.target.value })} required placeholder="H01" />
          </Field>
          <Field label="Name">
            <input className={inputClass} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
          </Field>
          <Field label="Address">
            <input className={inputClass} value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} required />
          </Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Monthly rent (Rs.)">
              <input type="number" min="0" step="0.01" className={inputClass} value={form.monthlyRent} onChange={(e) => setForm({ ...form, monthlyRent: e.target.value })} required />
            </Field>
            <Field label="Deposit (Rs.)">
              <input type="number" min="0" step="0.01" className={inputClass} value={form.deposit} onChange={(e) => setForm({ ...form, deposit: e.target.value })} />
            </Field>
          </div>
          <Field label="Status">
            <select className={inputClass} value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>
              {!(editing && occupancy.get(editing.id)) && <option value="AVAILABLE">Available</option>}
              <option value="OCCUPIED">Occupied</option>
              <option value="MAINTENANCE">Maintenance</option>
            </select>
          </Field>
          <Field label="Notes">
            <textarea className={inputClass} rows={3} value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} />
          </Field>
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" className={btnSecondary} onClick={() => setFormOpen(false)}>
              Cancel
            </button>
            <button type="submit" className={btnPrimary} disabled={saving}>
              {saving ? 'Saving…' : 'Save'}
            </button>
          </div>
        </form>
      </Modal>

      <Modal open={!!detail} title={detail ? `${detail.propertyCode} · ${detail.name}` : ''} onClose={() => setDetail(null)} wide>
        {detail && (
          <div className="space-y-4 text-sm">
            <StatusBadge status={detail.status} />
            <p className="text-ink-soft">{detail.address}</p>
            <p>Monthly rent {formatCurrency(detail.monthlyRent)}</p>
            {detail.deposit != null && <p>Deposit {formatCurrency(detail.deposit)}</p>}
            
            {/* Current occupants section */}
            {occupancy.get(detail.id) ? (
              <div className="space-y-3 rounded-lg border border-border bg-ink/5 p-3">
                <h3 className="font-display font-semibold text-ink">Who is in this {type === 'HOUSE' ? 'house' : 'room'}</h3>
                <div className="space-y-4">
                  {/* Tenant 1 */}
                  <div className="flex flex-col items-start gap-3 rounded-lg border border-border bg-surface p-4">
                    {occupancy.get(detail.id).tenant?.photoUrl && (
                      <img 
                        src={occupancy.get(detail.id).tenant.photoUrl} 
                        alt={occupancy.get(detail.id).tenant.fullName}
                        className="h-40 w-40 rounded-lg object-cover shadow-md"
                      />
                    )}
                    <div>
                      <p className="font-semibold text-ink">{occupancy.get(detail.id).tenant?.fullName}</p>
                      <p className="text-xs text-ink-soft">Since {formatDate(occupancy.get(detail.id).startDate)}</p>
                      <p className="text-xs text-ink-soft">{formatCurrency(occupancy.get(detail.id).monthlyRent)}/month</p>
                    </div>
                  </div>
                  
                  {/* Tenant 2 (if exists) */}
                  {occupancy.get(detail.id).tenant2 && (
                    <div className="flex flex-col items-start gap-3 rounded-lg border border-border bg-surface p-4">
                      {occupancy.get(detail.id).tenant2?.photoUrl && (
                        <img 
                          src={occupancy.get(detail.id).tenant2.photoUrl} 
                          alt={occupancy.get(detail.id).tenant2.fullName}
                          className="h-40 w-40 rounded-lg object-cover shadow-md"
                        />
                      )}
                      <div>
                        <p className="font-semibold text-ink">{occupancy.get(detail.id).tenant2?.fullName}</p>
                        <p className="text-xs text-ink-soft">Since {formatDate(occupancy.get(detail.id).startDate)}</p>
                        <p className="text-xs text-ink-soft">{formatCurrency(occupancy.get(detail.id).monthlyRent)}/month</p>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            ) : (
              <p className="text-ink-soft">No current tenant.</p>
            )}
            
            <h3 className="font-display font-semibold text-ink">Rental history</h3>
            {history.length === 0 ? (
              <p className="text-ink-soft">No rentals recorded yet.</p>
            ) : (
              <ul className="divide-y divide-border rounded-lg border border-border">
                {history.map((rental) => (
                  <li key={rental.id} className="flex flex-wrap items-center justify-between gap-2 px-3 py-2">
                    <span>
                      {rental.tenant?.fullName} · {formatDate(rental.startDate)} – {formatDate(rental.endDate)}
                    </span>
                    <StatusBadge status={rental.status} />
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}
      </Modal>

      <Modal open={!!changeOpen} title={occupancy.get(changeOpen?.id) ? 'Change tenant' : 'Assign tenant'} onClose={() => setChangeOpen(null)}>
        <p className="mb-4 text-sm text-ink-soft">
          {occupancy.get(changeOpen?.id)
            ? 'The current rental stays in history. A new rental is created for the incoming tenant.'
            : 'Creates a new rental for this property. It will not overwrite any past occupancy.'}
        </p>
        <form onSubmit={submitChange} className="space-y-3">
          <Field label="First tenant">
            <select className={inputClass} value={changeForm.newTenantId} onChange={(e) => setChangeForm({ ...changeForm, newTenantId: e.target.value })} required>
              <option value="">Select tenant</option>
              {tenants.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.fullName}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Second tenant (optional)">
            <select className={inputClass} value={changeForm.newTenant2Id} onChange={(e) => setChangeForm({ ...changeForm, newTenant2Id: e.target.value })}>
              <option value="">No second tenant</option>
              {tenants
                .filter((t) => t.id !== Number(changeForm.newTenantId))
                .map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.fullName}
                  </option>
                ))}
            </select>
          </Field>
          <Field label="Start date">
            <input type="date" className={inputClass} value={changeForm.newStartDate} onChange={(e) => setChangeForm({ ...changeForm, newStartDate: e.target.value })} required />
          </Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Monthly rent (Rs.)">
              <input type="number" min="0" step="0.01" className={inputClass} value={changeForm.monthlyRent} onChange={(e) => setChangeForm({ ...changeForm, monthlyRent: e.target.value })} />
            </Field>
            <Field label="Deposit (Rs.)">
              <input type="number" min="0" step="0.01" className={inputClass} value={changeForm.deposit} onChange={(e) => setChangeForm({ ...changeForm, deposit: e.target.value })} />
            </Field>
          </div>
          <Field label="Notes">
            <textarea className={inputClass} rows={2} value={changeForm.notes} onChange={(e) => setChangeForm({ ...changeForm, notes: e.target.value })} />
          </Field>
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" className={btnSecondary} onClick={() => setChangeOpen(null)}>
              Cancel
            </button>
            <button type="submit" className={btnPrimary} disabled={saving}>
              {saving ? 'Saving…' : occupancy.get(changeOpen?.id) ? 'Change tenant' : 'Assign tenant'}
            </button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={!!deleteTarget}
        title="Delete property?"
        message="This only works if the property has no rental history. Occupied properties should be ended or set to maintenance instead."
        confirmLabel="Delete"
        danger
        busy={busy}
        onClose={() => setDeleteTarget(null)}
        onConfirm={confirmDelete}
      />
    </div>
  )
}
