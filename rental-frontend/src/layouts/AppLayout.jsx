import { useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import {
  IconDashboard,
  IconHouse,
  IconDoor,
  IconTenant,
  IconRentals,
  IconPayments,
  IconSettings,
  IconSearch,
  IconLogout,
  IconMenu,
  IconKey,
} from '../components/icons'

const NAV_ITEMS = [
  { to: '/', label: 'Dashboard', icon: IconDashboard, end: true },
  { to: '/houses', label: 'Houses', icon: IconHouse },
  { to: '/rooms', label: 'Rooms', icon: IconDoor },
  { to: '/tenants', label: 'Tenants', icon: IconTenant },
  { to: '/rentals', label: 'Rentals', icon: IconRentals },
  { to: '/payments', label: 'Rent Payments', icon: IconPayments },
  { to: '/settings', label: 'Settings', icon: IconSettings },
]

const MOBILE_NAV_ITEMS = NAV_ITEMS.slice(0, 6)

export default function AppLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [search, setSearch] = useState('')

  return (
    <div className="min-h-screen bg-paper">
      {/* ---------- Desktop sidebar ---------- */}
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-60 flex-col border-r border-border bg-brand text-white/90 md:flex">
        <Brand />
        <nav className="flex-1 space-y-1 px-3 py-4">
          {NAV_ITEMS.map((item) => (
            <SidebarLink key={item.to} {...item} />
          ))}
        </nav>
        <div className="border-t border-white/10 p-3">
          <p className="px-3 text-xs text-white/50">Signed in as</p>
          <p className="truncate px-3 pb-1 text-sm font-medium text-white">{user?.username}</p>
        </div>
      </aside>

      {/* ---------- Mobile sidebar drawer ---------- */}
      {sidebarOpen && (
        <div className="fixed inset-0 z-40 md:hidden">
          <div className="absolute inset-0 bg-ink/40" onClick={() => setSidebarOpen(false)} />
          <aside className="absolute inset-y-0 left-0 w-64 bg-brand text-white/90">
            <Brand />
            <nav className="space-y-1 px-3 py-4">
              {NAV_ITEMS.map((item) => (
                <SidebarLink key={item.to} {...item} onClick={() => setSidebarOpen(false)} />
              ))}
            </nav>
          </aside>
        </div>
      )}

      {/* ---------- Main column ---------- */}
      <div className="flex min-h-screen flex-col md:pl-60">
        <header className="sticky top-0 z-20 flex items-center gap-3 border-b border-border bg-paper/90 px-4 py-3 backdrop-blur md:px-6">
          <button
            className="rounded-lg p-2 text-ink-soft hover:bg-ink/5 md:hidden"
            onClick={() => setSidebarOpen(true)}
            aria-label="Open menu"
          >
            <IconMenu className="h-5 w-5" />
          </button>

          <form
            className="relative flex-1 max-w-md"
            onSubmit={(e) => {
              e.preventDefault()
              const q = search.trim()
              navigate(q ? `/search?q=${encodeURIComponent(q)}` : '/search')
            }}
          >
            <IconSearch className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-faint" />
            <input
              type="search"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search tenants or properties…"
              className="w-full rounded-lg border border-border bg-surface py-2 pl-9 pr-3 text-sm placeholder:text-ink-faint focus:border-brand focus:outline-none"
            />
          </form>

          <div className="ml-auto flex items-center gap-2">
            <span className="hidden text-sm text-ink-soft sm:inline">{user?.username}</span>
            <button
              onClick={() => {
                logout()
                navigate('/login', { replace: true })
              }}
              className="flex items-center gap-1.5 rounded-lg border border-border px-3 py-2 text-sm font-medium text-ink-soft hover:border-danger/40 hover:text-danger"
            >
              <IconLogout className="h-4 w-4" />
              <span className="hidden sm:inline">Log out</span>
            </button>
          </div>
        </header>

        <main className="flex-1 px-4 pb-24 pt-5 md:px-6 md:pb-10">
          <Outlet />
        </main>
      </div>

      {/* ---------- Mobile bottom nav ---------- */}
      <nav className="fixed inset-x-0 bottom-0 z-20 flex border-t border-border bg-surface md:hidden">
        {MOBILE_NAV_ITEMS.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            className={({ isActive }) =>
              `flex flex-1 flex-col items-center gap-1 py-2.5 text-[11px] font-medium ${
                isActive ? 'text-brand' : 'text-ink-faint'
              }`
            }
          >
            <item.icon className="h-5 w-5" />
            {item.label.split(' ')[0]}
          </NavLink>
        ))}
      </nav>
    </div>
  )
}

function Brand() {
  return (
    <div className="flex items-center gap-2.5 px-5 py-5">
      <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-accent/20 text-accent-light">
        <IconKey className="h-4.5 w-4.5" />
      </div>
      <div>
        <p className="font-display text-base font-semibold leading-none text-white">Rental Manager</p>
      </div>
    </div>
  )
}

function SidebarLink({ to, label, icon: Icon, end, onClick }) {
  return (
    <NavLink
      to={to}
      end={end}
      onClick={onClick}
      className={({ isActive }) =>
        `flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors ${
          isActive
            ? 'bg-white/10 text-white'
            : 'text-white/65 hover:bg-white/5 hover:text-white'
        }`
      }
    >
      <Icon className="h-[18px] w-[18px]" />
      {label}
    </NavLink>
  )
}
