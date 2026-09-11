import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchDashboard } from '../services/dashboardService'
import { formatCurrency, formatDate } from '../utils/format'
import {
  IconHouse,
  IconDoor,
  IconTenant,
  IconDoor as IconOccupied,
  IconAlert,
} from '../components/icons'

export default function Dashboard() {
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  function load() {
    setError('')
    setData(null)
    fetchDashboard()
      .then(setData)
      .catch(() => setError('Could not load the dashboard.'))
  }

  useEffect(() => {
    load()
  }, [])

  if (error) {
    return (
      <div className="rounded-lg bg-danger-soft p-4 text-sm text-danger">
        <p>{error}</p>
        <button type="button" className="mt-3 rounded-lg bg-brand px-3 py-2 text-white" onClick={load}>
          Try again
        </button>
      </div>
    )
  }

  if (!data) {
    return <DashboardSkeleton />
  }

  const totalProperties = data.totalHouses + data.totalRooms
  const paidTotal = Number(data.currentMonthPaidAmount ?? 0) + Number(data.currentMonthUnpaidAmount ?? 0)
  const paidPct = paidTotal > 0 ? Math.round((Number(data.currentMonthPaidAmount ?? 0) / paidTotal) * 100) : 0

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-2xl font-semibold text-ink">Dashboard</h1>
        <p className="mt-0.5 text-sm text-ink-soft">
          {totalProperties} propert{totalProperties === 1 ? 'y' : 'ies'} · {data.totalTenants} tenant
          {data.totalTenants === 1 ? '' : 's'}
        </p>
      </div>

      {/* ---------- Category overview ---------- */}
      <section className="space-y-4">
        <div className="rounded-xl border border-border bg-surface p-5 shadow-card">
          <div className="flex items-center gap-2">
            <IconDoor className="h-4 w-4 text-brand" />
            <h2 className="font-display text-base font-semibold text-ink">Rooms</h2>
          </div>
          <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
            <OverviewMetric label="All" value={data.totalRooms} />
            <OverviewMetric label="Occupied" value={data.roomsOccupied ?? 0} />
            <OverviewMetric label="Available" value={data.roomsAvailable ?? 0} />
            <OverviewMetric label="Tenants" value={data.roomsTenants ?? 0} />
          </div>
        </div>

        <div className="rounded-xl border border-border bg-surface p-5 shadow-card">
          <div className="flex items-center gap-2">
            <IconHouse className="h-4 w-4 text-brand" />
            <h2 className="font-display text-base font-semibold text-ink">Houses</h2>
          </div>
          <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
            <OverviewMetric label="All" value={data.totalHouses} />
            <OverviewMetric label="Occupied" value={data.housesOccupied ?? 0} />
            <OverviewMetric label="Available" value={data.housesAvailable ?? 0} />
            <OverviewMetric label="Tenants" value={data.housesTenants ?? 0} />
          </div>
        </div>
      </section>

      {/* ---------- This month ---------- */}
      <div className="rounded-xl border border-border bg-surface p-5 shadow-card">
        <div className="flex items-center justify-between">
          <h2 className="font-display text-base font-semibold text-ink">This month</h2>
          <span className="text-sm text-ink-soft">{paidPct}% collected</span>
        </div>

        <div className="mt-3 h-2.5 w-full overflow-hidden rounded-full bg-warning-soft">
          <div
            className="h-full rounded-full bg-success transition-all"
            style={{ width: `${paidPct}%` }}
          />
        </div>

        <div className="mt-4 flex flex-wrap gap-6">
          <div>
            <p className="font-display text-2xl font-semibold tabular text-success">
              {formatCurrency(data.currentMonthPaidAmount)}
            </p>
            <p className="text-xs text-ink-soft">Paid ({data.currentMonthPaid})</p>
          </div>
          <div>
            <p className="font-display text-2xl font-semibold tabular text-warning">
              {formatCurrency(data.currentMonthUnpaidAmount)}
            </p>
            <p className="text-xs text-ink-soft">Unpaid ({data.currentMonthUnpaid})</p>
          </div>
        </div>
      </div>

      {/* ---------- Rent pending ledger ---------- */}
      <div className="rounded-xl border border-border bg-surface shadow-card">
        <div className="flex items-center gap-2 border-b border-border px-5 py-4">
          <IconAlert className="h-4 w-4 text-warning" />
          <h2 className="font-display text-base font-semibold text-ink">Rent Pending</h2>
          <Link to="/payments" className="ml-auto text-xs font-medium text-ink-soft hover:text-ink">
            Open payments
          </Link>
          {data.rentPending.length > 0 && (
            <span className="rounded-full bg-warning-soft px-2.5 py-0.5 text-xs font-medium text-warning">
              {data.rentPending.length}
            </span>
          )}
        </div>

        {data.rentPending.length === 0 ? (
          <p className="px-5 py-8 text-center text-sm text-ink-soft">
            Nothing pending — everyone's paid up this month.
          </p>
        ) : (
          <ul className="divide-y divide-border">
            {data.rentPending.map((item) => (
              <li
                key={item.rentPaymentId}
                className={`flex items-center gap-4 border-l-[3px] px-5 py-3.5 ${
                  item.overdue ? 'border-l-danger' : 'border-l-warning'
                }`}
              >
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-ink">{item.tenantName}</p>
                  <p className="text-xs text-ink-soft">
                    {item.propertyCode} · {item.propertyName} · due {formatDate(item.dueDate)}
                  </p>
                </div>
                <div className="text-right">
                  <p className="font-mono text-sm font-semibold tabular text-ink">
                    {formatCurrency(item.balance)}
                  </p>
                  <p className={`text-xs ${item.overdue ? 'text-danger' : 'text-warning'}`}>
                    {item.overdue ? 'Overdue' : 'Pending'}
                  </p>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}

function OverviewMetric({ label, value }) {
  return (
    <div className="rounded-xl border border-border bg-ink/[0.03] px-4 py-3">
      <p className="text-xs font-medium uppercase tracking-wide text-ink-soft">{label}</p>
      <p className="mt-2 font-display text-2xl font-semibold tabular text-ink">{value}</p>
    </div>
  )
}

function DashboardSkeleton() {
  return (
    <div className="space-y-6 animate-pulse">
      <div className="h-7 w-40 rounded bg-ink/10" />
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="h-24 rounded-xl border border-border bg-ink/5" />
        ))}
      </div>
      <div className="h-40 rounded-xl border border-border bg-ink/5" />
      <div className="h-56 rounded-xl border border-border bg-ink/5" />
    </div>
  )
}
