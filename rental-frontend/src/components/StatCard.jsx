export default function StatCard({ label, value, sublabel, icon: Icon, tone = 'brand' }) {
  const toneClasses = {
    brand: 'text-brand bg-brand/[0.06]',
    success: 'text-success bg-success-soft',
    warning: 'text-warning bg-warning-soft',
    danger: 'text-danger bg-danger-soft',
    info: 'text-info bg-info-soft',
  }[tone]

  return (
    <div className="rounded-xl border border-border bg-surface p-5 shadow-card">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm text-ink-soft">{label}</p>
          <p className="mt-1.5 font-display text-3xl font-semibold tabular text-ink">{value}</p>
          {sublabel && <p className="mt-1 text-xs text-ink-faint">{sublabel}</p>}
        </div>
        {Icon && (
          <div className={`rounded-lg p-2 ${toneClasses}`}>
            <Icon className="h-4 w-4" />
          </div>
        )}
      </div>
    </div>
  )
}
