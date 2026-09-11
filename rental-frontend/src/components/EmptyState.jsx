export default function EmptyState({ title, message, action }) {
  return (
    <div className="rounded-xl border border-dashed border-border bg-surface/60 px-5 py-12 text-center">
      <p className="font-display text-base font-semibold text-ink">{title}</p>
      {message && <p className="mx-auto mt-1.5 max-w-sm text-sm text-ink-soft">{message}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  )
}
