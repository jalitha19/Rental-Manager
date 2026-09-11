import { useEffect, useState } from 'react'
import { Navigate, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { IconKey } from '../components/icons'
import { createFirstAdmin, getFirstAdminStatus } from '../services/authService'

export default function Login() {
  const { login, user, loading } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const [needsSetup, setNeedsSetup] = useState(false)
  const [checkingSetup, setCheckingSetup] = useState(true)
  const [adminUsername, setAdminUsername] = useState('')
  const [adminPassword, setAdminPassword] = useState('')
  const [setupError, setSetupError] = useState('')
  const [setupSubmitting, setSetupSubmitting] = useState(false)

  useEffect(() => {
    async function loadSetupStatus() {
      try {
        const status = await getFirstAdminStatus()
        setNeedsSetup(Boolean(status?.needsSetup))
      } catch {
        setNeedsSetup(false)
      } finally {
        setCheckingSetup(false)
      }
    }

    loadSetupStatus()
  }, [])

  if (loading || checkingSetup) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-paper">
        <p className="text-sm text-ink-soft">Loading…</p>
      </div>
    )
  }

  if (user) {
    return <Navigate to={location.state?.from?.pathname ?? '/'} replace />
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await login(username, password)
      const redirectTo = location.state?.from?.pathname ?? '/'
      navigate(redirectTo, { replace: true })
    } catch {
      setError('Incorrect username or password.')
    } finally {
      setSubmitting(false)
    }
  }

  async function handleFirstAdminSubmit(e) {
    e.preventDefault()
    setSetupError('')
    setSetupSubmitting(true)
    try {
      await createFirstAdmin(adminUsername, adminPassword)
      setNeedsSetup(false)
      setUsername(adminUsername)
      setPassword(adminPassword)
      setError('')
    } catch (err) {
      const message = err?.response?.data?.message || 'Unable to create the first admin account.'
      setSetupError(message)
    } finally {
      setSetupSubmitting(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-paper px-4">
      <div className="w-full max-w-sm">
        <div className="mb-8 flex flex-col items-center text-center">
          <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-brand text-accent-light">
            <IconKey className="h-6 w-6" />
          </div>
          <h1 className="font-display text-2xl font-semibold text-ink">Rental Manager</h1>
          <p className="mt-1 text-sm text-ink-soft">
            {needsSetup ? 'Create your first administrator' : 'Sign in to manage your properties'}
          </p>
        </div>

        {needsSetup ? (
          <form onSubmit={handleFirstAdminSubmit} className="rounded-xl border border-border bg-surface p-6 shadow-card">
            <label className="block text-sm font-medium text-ink">
              Choose username
              <input
                type="text"
                autoFocus
                autoComplete="username"
                value={adminUsername}
                onChange={(e) => setAdminUsername(e.target.value)}
                className="mt-1.5 w-full rounded-lg border border-border px-3 py-2.5 text-sm focus:border-brand focus:outline-none"
                required
                minLength="3"
              />
            </label>

            <label className="mt-4 block text-sm font-medium text-ink">
              Choose password
              <input
                type="password"
                autoComplete="new-password"
                value={adminPassword}
                onChange={(e) => setAdminPassword(e.target.value)}
                className="mt-1.5 w-full rounded-lg border border-border px-3 py-2.5 text-sm focus:border-brand focus:outline-none"
                required
                minLength="6"
              />
            </label>

            {setupError && (
              <p className="mt-4 rounded-lg bg-danger-soft px-3 py-2 text-sm text-danger" role="alert">
                {setupError}
              </p>
            )}

            <button
              type="submit"
              disabled={setupSubmitting}
              className="mt-6 w-full rounded-lg bg-brand py-2.5 text-sm font-semibold text-white transition-colors hover:bg-brand-light disabled:opacity-60"
            >
              {setupSubmitting ? 'Creating admin…' : 'Create first user'}
            </button>
          </form>
        ) : (
          <form onSubmit={handleSubmit} className="rounded-xl border border-border bg-surface p-6 shadow-card">
            <label className="block text-sm font-medium text-ink">
              Username
              <input
                type="text"
                autoFocus
                autoComplete="username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="mt-1.5 w-full rounded-lg border border-border px-3 py-2.5 text-sm focus:border-brand focus:outline-none"
                required
              />
            </label>

            <label className="mt-4 block text-sm font-medium text-ink">
              Password
              <input
                type="password"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="mt-1.5 w-full rounded-lg border border-border px-3 py-2.5 text-sm focus:border-brand focus:outline-none"
                required
              />
            </label>

            {error && (
              <p className="mt-4 rounded-lg bg-danger-soft px-3 py-2 text-sm text-danger" role="alert">
                {error}
              </p>
            )}

            <button
              type="submit"
              disabled={submitting}
              className="mt-6 w-full rounded-lg bg-brand py-2.5 text-sm font-semibold text-white transition-colors hover:bg-brand-light disabled:opacity-60"
            >
              {submitting ? 'Signing in…' : 'Sign in'}
            </button>
          </form>
        )}
      </div>
    </div>
  )
}
