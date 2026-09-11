import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listTenants, searchTenants, createTenant, deleteTenant } from '../services/tenantService'
import { getErrorMessage } from '../services/api'
import { uploadTenantPhoto, deleteTenantPhoto } from '../services/supabaseStorage'
import { useToast } from '../contexts/ToastContext'
import EmptyState from '../components/EmptyState'
import Modal from '../components/Modal'
import ConfirmDialog from '../components/ConfirmDialog'
import Field, { inputClass, btnPrimary, btnSecondary } from '../components/Field'

const EMPTY = {
  tenantType: 'INDIVIDUAL',
  fullName: '',
  nicNumber: '',
  contactPerson: '',
  address: '',
  notes: '',
  phoneNumber: '',
  phoneLabel: 'Mobile',
  photoFile: null,
}

export default function Tenants() {
  const { push } = useToast()
  const [items, setItems] = useState([])
  const [query, setQuery] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState(EMPTY)
  const [saving, setSaving] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [busy, setBusy] = useState(false)

  async function load(search = query) {
    setLoading(true)
    setError('')
    try {
      const data = search.trim() ? await searchTenants(search.trim()) : await listTenants()
      setItems(data)
    } catch (err) {
      setError(getErrorMessage(err, 'Could not load tenants.'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load('')
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function save(e) {
    e.preventDefault()
    setSaving(true)
    try {
      const photoUrl = await uploadTenantPhoto(form.photoFile)
      const phones = form.phoneNumber.trim()
        ? [{ phoneNumber: form.phoneNumber.trim(), label: form.phoneLabel || null }]
        : []
      await createTenant({
        tenantType: form.tenantType,
        fullName: form.fullName.trim(),
        nicNumber: form.nicNumber.trim() || null,
        contactPerson: form.tenantType === 'COMPANY' ? form.contactPerson.trim() || null : null,
        address: form.address.trim() || null,
        notes: form.notes.trim() || null,
        photoUrl,
        phones,
      })
      push('Tenant added')
      setOpen(false)
      setForm(EMPTY)
      await load()
    } catch (err) {
      push(getErrorMessage(err, 'Could not save tenant.'), 'error')
    } finally {
      setSaving(false)
    }
  }

  async function confirmDelete() {
    setBusy(true)
    try {
      const tenantToDelete = deleteTarget
      
      // Delete tenant from database first
      await deleteTenant(tenantToDelete.id)
      push('Tenant deleted from database')
      
      // Then delete photo from storage (non-blocking)
      if (tenantToDelete.photoUrl) {
        try {
          await deleteTenantPhoto(tenantToDelete.photoUrl)
          push('Photo also deleted from storage', 'success')
        } catch (photoErr) {
          // Photo deletion failed, but tenant is already deleted from DB
          console.error('Photo deletion failed:', photoErr)
          push('Tenant deleted, but photo may still be in storage. Check browser console for details.', 'warning')
        }
      }
      
      setDeleteTarget(null)
      await load()
    } catch (err) {
      push(getErrorMessage(err, 'Could not delete tenant.'), 'error')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="space-y-5">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="font-display text-2xl font-semibold text-ink">Tenants</h1>
          <p className="mt-0.5 text-sm text-ink-soft">{items.length} people and companies</p>
        </div>
        <button type="button" className={btnPrimary} onClick={() => { setForm(EMPTY); setOpen(true) }}>
          Add tenant
        </button>
      </div>

      <form
        onSubmit={(e) => {
          e.preventDefault()
          load(query)
        }}
        className="flex gap-2"
      >
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search by name, NIC, or phone"
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
            <div key={i} className="h-20 rounded-xl border border-border bg-ink/5" />
          ))}
        </div>
      ) : items.length === 0 ? (
        <EmptyState
          title="No tenants found"
          message="Add a tenant, or try a different name, NIC, or phone number."
        />
      ) : (
        <ul className="space-y-3">
          {items.map((tenant) => (
            <li key={tenant.id} className="rounded-xl border border-border bg-surface p-4 shadow-card">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <p className="font-semibold text-ink">{tenant.fullName}</p>
                  <p className="text-xs text-ink-soft">
                    {tenant.tenantType === 'COMPANY' ? 'Company' : 'Individual'}
                    {tenant.nicNumber ? ` · NIC ${tenant.nicNumber}` : ''}
                    {tenant.phones?.[0] ? ` · ${tenant.phones[0].phoneNumber}` : ''}
                  </p>
                </div>
                <div className="flex gap-2">
                  <Link to={`/tenants/${tenant.id}`} className={`${btnSecondary} inline-flex items-center`}>
                    Details
                  </Link>
                  <button
                    type="button"
                    className="rounded-lg border border-border px-4 py-2.5 text-sm font-medium text-danger hover:bg-danger-soft"
                    onClick={() => setDeleteTarget(tenant)}
                  >
                    Delete
                  </button>
                </div>
              </div>
            </li>
          ))}
        </ul>
      )}

      <Modal open={open} title="Add tenant" onClose={() => setOpen(false)}>
        <form onSubmit={save} className="space-y-3">
          <Field label="Type">
            <select className={inputClass} value={form.tenantType} onChange={(e) => setForm({ ...form, tenantType: e.target.value })}>
              <option value="INDIVIDUAL">Individual</option>
              <option value="COMPANY">Company</option>
            </select>
          </Field>
          <Field label={form.tenantType === 'COMPANY' ? 'Company name' : 'Full name'}>
            <input className={inputClass} value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} required />
          </Field>
          <Field label="NIC / ID">
            <input className={inputClass} value={form.nicNumber} onChange={(e) => setForm({ ...form, nicNumber: e.target.value })} />
          </Field>
          {form.tenantType === 'COMPANY' && (
            <Field label="Contact person">
              <input className={inputClass} value={form.contactPerson} onChange={(e) => setForm({ ...form, contactPerson: e.target.value })} />
            </Field>
          )}
          <Field label="Address">
            <input className={inputClass} value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} />
          </Field>
          <Field label="Notes">
            <textarea className={inputClass} rows={2} value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} />
          </Field>
          <Field label="Photo" hint="JPG, PNG, or another image up to 5 MB.">
            <input
              type="file"
              accept="image/*"
              className={inputClass}
              onChange={(e) => setForm({ ...form, photoFile: e.target.files?.[0] ?? null })}
            />
          </Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Phone">
              <input className={inputClass} value={form.phoneNumber} onChange={(e) => setForm({ ...form, phoneNumber: e.target.value })} />
            </Field>
            <Field label="Phone label">
              <input className={inputClass} value={form.phoneLabel} onChange={(e) => setForm({ ...form, phoneLabel: e.target.value })} />
            </Field>
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" className={btnSecondary} onClick={() => setOpen(false)}>Cancel</button>
            <button type="submit" className={btnPrimary} disabled={saving}>{saving ? 'Saving…' : 'Save'}</button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={!!deleteTarget}
        title="Delete tenant?"
        message="Tenants with rental history cannot be deleted, so past payments stay intact."
        confirmLabel="Delete"
        danger
        busy={busy}
        onClose={() => setDeleteTarget(null)}
        onConfirm={confirmDelete}
      />
    </div>
  )
}
