export default function Field({ label, children, hint }) {
  return (
    <label className="block text-sm font-medium text-ink">
      {label}
      <div className="mt-1.5">{children}</div>
      {hint && <p className="mt-1 text-xs font-normal text-ink-faint">{hint}</p>}
    </label>
  )
}

export const inputClass =
  'w-full rounded-lg border border-border bg-surface px-3 py-2.5 text-sm text-ink placeholder:text-ink-faint focus:border-brand focus:outline-none'

export const btnPrimary =
  'rounded-lg bg-brand px-4 py-2.5 text-sm font-semibold text-white hover:bg-brand-light disabled:opacity-60'

export const btnSecondary =
  'rounded-lg border border-border px-4 py-2.5 text-sm font-medium text-ink-soft hover:bg-ink/5'
