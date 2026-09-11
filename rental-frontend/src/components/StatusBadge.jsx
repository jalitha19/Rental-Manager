const VARIANTS = {
  PAID: 'bg-success-soft text-success',
  AVAILABLE: 'bg-ink-faint/15 text-ink-soft',
  ACTIVE: 'bg-success-soft text-success',
  COMPLETED: 'bg-ink-faint/20 text-ink-soft',

  UNPAID: 'bg-danger-soft text-danger',
  PARTIALLY_PAID: 'bg-warning-soft text-warning',
  MAINTENANCE: 'bg-warning-soft text-warning',

  OVERDUE: 'bg-danger-soft text-danger',
  CANCELLED: 'bg-danger-soft text-danger',

  OCCUPIED: 'bg-info-soft text-info',
}

const DOTS = {
  PAID: '🟢',
  UNPAID: '🔴',
  OVERDUE: '🟠',
  PARTIALLY_PAID: '🟠',
  OCCUPIED: '🔵',
  AVAILABLE: '⚪',
  ACTIVE: '🔵',
  MAINTENANCE: '🟠',
  COMPLETED: '⚪',
  CANCELLED: '🔴',
}

const LABELS = {
  PARTIALLY_PAID: 'Partially paid',
}

export default function StatusBadge({ status }) {
  const classes = VARIANTS[status] ?? 'bg-ink-faint/20 text-ink-soft'
  const label = LABELS[status] ?? toTitleCase(status)
  const dot = DOTS[status] ?? '⚪'

  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium ${classes}`}
    >
      <span aria-hidden="true">{dot}</span>
      {label}
    </span>
  )
}

function toTitleCase(value = '') {
  return value
    .toLowerCase()
    .split('_')
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ')
}
