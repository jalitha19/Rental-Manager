import { useState } from 'react'
import { changePassword } from '../services/authService'
import { getErrorMessage } from '../services/api'
import { useAuth } from '../contexts/AuthContext'
import { useToast } from '../contexts/ToastContext'
import Field, { inputClass, btnPrimary } from '../components/Field'

export default function Settings() {
  const { user } = useAuth()
  const { push } = useToast()
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [saving, setSaving] = useState(false)

  async function submit(e) {
    e.preventDefault()
    if (newPassword !== confirm) {
      push('New passwords do not match.', 'error')
      return
    }
    setSaving(true)
    try {
      await changePassword(currentPassword, newPassword)
      push('Password updated')
      setCurrentPassword('')
      setNewPassword('')
      setConfirm('')
    } catch (err) {
      push(getErrorMessage(err, 'Could not change password.'), 'error')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="mx-auto max-w-lg space-y-5">
      <div>
        <h1 className="font-display text-2xl font-semibold text-ink">Settings</h1>
        <p className="mt-0.5 text-sm text-ink-soft">Signed in as {user?.username}</p>
      </div>

      <form onSubmit={submit} className="space-y-3 rounded-xl border border-border bg-surface p-5 shadow-card">
        <h2 className="font-display font-semibold text-ink">Change password</h2>
        <Field label="Current password">
          <input type="password" className={inputClass} value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} required autoComplete="current-password" />
        </Field>
        <Field label="New password" hint="At least 6 characters.">
          <input type="password" className={inputClass} value={newPassword} onChange={(e) => setNewPassword(e.target.value)} required minLength={6} autoComplete="new-password" />
        </Field>
        <Field label="Confirm new password">
          <input type="password" className={inputClass} value={confirm} onChange={(e) => setConfirm(e.target.value)} required minLength={6} autoComplete="new-password" />
        </Field>
        <button type="submit" className={btnPrimary} disabled={saving}>
          {saving ? 'Saving…' : 'Update password'}
        </button>
      </form>
    </div>
  )
}
