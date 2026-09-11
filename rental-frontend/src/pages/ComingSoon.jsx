export default function ComingSoon({ title }) {
  return (
    <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-border bg-surface/50 py-20 text-center">
      <h1 className="font-display text-xl font-semibold text-ink">{title}</h1>
      <p className="mt-1.5 max-w-xs text-sm text-ink-soft">
        This page is built in the next step, once the rest of the frontend is wired up.
      </p>
    </div>
  )
}
