export function formatCurrency(amount) {
  const value = Number(amount ?? 0)
  return `Rs. ${value.toLocaleString('en-LK', { minimumFractionDigits: 0, maximumFractionDigits: 2 })}`
}

function parseDate(dateStr) {
  if (!dateStr) return null
  const datePart = String(dateStr).slice(0, 10)
  const [year, month, day] = datePart.split('-').map(Number)
  if (!year || !month) return null
  return new Date(year, month - 1, day || 1)
}

export function formatDate(dateStr) {
  const date = parseDate(dateStr)
  if (!date) return '—'
  return date.toLocaleDateString('en-GB', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  })
}

export function formatMonth(dateStr) {
  const date = parseDate(dateStr)
  if (!date) return '—'
  return date.toLocaleDateString('en-GB', { month: 'short', year: 'numeric' })
}

export function todayIso() {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}

export function firstOfMonthIso(date = new Date()) {
  const month = String(date.getMonth() + 1).padStart(2, '0')
  return `${date.getFullYear()}-${month}-01`
}
