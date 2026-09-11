import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { searchTenants } from '../services/tenantService'
import { searchProperties } from '../services/propertyService'
import { getErrorMessage } from '../services/api'
import StatusBadge from '../components/StatusBadge'
import EmptyState from '../components/EmptyState'
import { formatCurrency } from '../utils/format'
import { inputClass, btnSecondary } from '../components/Field'

export default function Search() {
  const [params, setParams] = useSearchParams()
  const qParam = params.get('q') ?? ''
  const [query, setQuery] = useState(qParam)
  const [tenants, setTenants] = useState([])
  const [properties, setProperties] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    setQuery(qParam)
    const q = qParam.trim()
    if (!q) {
      setTenants([])
      setProperties([])
      setLoading(false)
      return
    }
    let cancelled = false
    setLoading(true)
    setError('')
    Promise.all([searchTenants(q), searchProperties(q)])
      .then(([t, p]) => {
        if (cancelled) return
        setTenants(t)
        setProperties(p)
      })
      .catch((err) => {
        if (!cancelled) setError(getErrorMessage(err, 'Search failed.'))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [qParam])

  return (
    <div className="space-y-5">
      <div>
        <h1 className="font-display text-2xl font-semibold text-ink">Search</h1>
        <p className="mt-0.5 text-sm text-ink-soft">Find tenants by name, NIC, or phone — and properties by code, name, or address.</p>
      </div>

      <form
        onSubmit={(e) => {
          e.preventDefault()
          setParams(query.trim() ? { q: query.trim() } : {}, { replace: true })
        }}
        className="flex gap-2"
      >
        <input className={inputClass} value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Name, NIC, phone, or property" />
        <button type="submit" className={btnSecondary}>Search</button>
      </form>

      {error && <p className="rounded-lg bg-danger-soft p-3 text-sm text-danger">{error}</p>}

      {loading ? (
        <div className="h-32 animate-pulse rounded-xl border border-border bg-ink/5" />
      ) : !qParam.trim() ? (
        <EmptyState title="Type something to search" message="Try a tenant name, NIC, phone number, or a house/room code." />
      ) : (
        <>
          <section className="space-y-3">
            <h2 className="font-display font-semibold text-ink">Tenants</h2>
            {tenants.length === 0 ? (
              <p className="text-sm text-ink-soft">No matching tenants.</p>
            ) : (
              <ul className="space-y-2">
                {tenants.map((tenant) => (
                  <li key={tenant.id} className="rounded-xl border border-border bg-surface p-4 shadow-card">
                    <Link to={`/tenants/${tenant.id}`} className="font-semibold text-ink hover:underline">
                      {tenant.fullName}
                    </Link>
                    <p className="text-xs text-ink-soft">
                      {tenant.tenantType === 'COMPANY' ? 'Company' : 'Individual'}
                      {tenant.nicNumber ? ` · NIC ${tenant.nicNumber}` : ''}
                      {tenant.phones?.[0] ? ` · ${tenant.phones[0].phoneNumber}` : ''}
                    </p>
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section className="space-y-3">
            <h2 className="font-display font-semibold text-ink">Properties</h2>
            {properties.length === 0 ? (
              <p className="text-sm text-ink-soft">No matching houses or rooms.</p>
            ) : (
              <ul className="space-y-2">
                {properties.map((property) => (
                  <li key={property.id} className="rounded-xl border border-border bg-surface p-4 shadow-card">
                    <div className="flex items-center justify-between gap-3">
                      <div>
                        <p className="font-semibold text-ink">
                          {property.propertyCode} · {property.name}
                        </p>
                        <p className="text-xs text-ink-soft">
                          {property.type === 'HOUSE' ? 'House' : 'Room'} · {property.address} · {formatCurrency(property.monthlyRent)}
                        </p>
                      </div>
                      <StatusBadge status={property.status} />
                    </div>
                    <Link
                      to={`${property.type === 'HOUSE' ? '/houses' : '/rooms'}?q=${encodeURIComponent(property.propertyCode)}`}
                      className="mt-2 inline-block text-sm text-ink-soft hover:text-ink"
                    >
                      Open {property.type === 'HOUSE' ? 'Houses' : 'Rooms'}
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </>
      )}
    </div>
  )
}
