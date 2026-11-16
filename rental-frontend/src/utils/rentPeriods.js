/**
 * Rent history uses inclusive calendar dates. Gaps are allowed; overlapping
 * prices are not. A monthly invoice is priced by the first day with a defined
 * rent rate in that month. Mid-month changes are not prorated automatically.
 */
export const monthOf = (date) => String(date ?? '').slice(0, 7)

export function nextMonth(month) {
  if (!/^\d{4}-(0[1-9]|1[0-2])$/.test(month)) return ''
  const [year, value] = month.split('-').map(Number)
  const next = new Date(Date.UTC(year, value, 1))
  return `${next.getUTCFullYear()}-${String(next.getUTCMonth() + 1).padStart(2, '0')}`
}

export function displayDate(iso) {
  const match = String(iso ?? '').match(/^(\d{4})-(\d{2})-(\d{2})/)
  return match ? `${match[3]}/${match[2]}/${match[1]}` : ''
}

/** Accept both the calendar input's ISO value and DD/MM/YYYY pasted values. */
export function parseDisplayDate(value) {
  const raw = String(value ?? '').trim()
  let year, month, day
  let iso = raw.match(/^(\d{4})-(\d{2})-(\d{2})$/)
  if (iso) {
    ;[, year, month, day] = iso
  } else {
    const typed = raw.match(/^(\d{2})\/(\d{2})\/(\d{4})$/)
    if (!typed) return null
    ;[, day, month, year] = typed
  }
  const date = new Date(Date.UTC(Number(year), Number(month) - 1, Number(day)))
  if (Number(year) < 1900 || date.getUTCFullYear() !== Number(year)
      || date.getUTCMonth() !== Number(month) - 1 || date.getUTCDate() !== Number(day)) return null
  return `${year}-${month}-${day}`
}

export function endOfMonthIso(month) {
  if (!/^\d{4}-(0[1-9]|1[0-2])$/.test(month)) return ''
  const [year, value] = month.split('-').map(Number)
  const last = new Date(Date.UTC(year, value, 0))
  return `${month}-${String(last.getUTCDate()).padStart(2, '0')}`
}

export function initialRentPeriods(startDate, rent) {
  return [{ startDate: parseDisplayDate(startDate) ?? '', endDate: null, monthlyRent: rent ?? '' }]
}

/** The API serves real daily dates (V8), or legacy first-of-month V7 values. */
export function scheduleToForm(periods, originalStart, currentRent) {
  const rows = periods?.length ? periods : initialRentPeriods(originalStart, currentRent)
  const historicalPeriods = rows.slice(0, -1).map((entry) => ({
    startDate: parseDisplayDate(entry.startDate ?? entry.startMonth) ?? '',
    // V7 endMonth was inclusive; translate it to the last day of that month.
    endDate: parseDisplayDate(entry.endDate ?? (entry.endMonth ? endOfMonthIso(monthOf(entry.endMonth)) : '')) ?? '',
    monthlyRent: String(entry.monthlyRent ?? ''),
  }))
  const latest = rows.at(-1)
  const currentStart = parseDisplayDate(latest.startDate ?? latest.startMonth) ?? parseDisplayDate(originalStart) ?? ''
  return {
    currentStartText: currentStart,
    monthlyRent: String(latest.monthlyRent ?? currentRent ?? ''),
    historicalPeriods,
  }
}

/** Leave a new period blank: in real life, the next rate may start after a gap. */
export function appendHistoricalPeriod(periods = []) {
  if (periods.length >= 99) return periods
  return [...periods, { startDate: '', endDate: '', monthlyRent: '' }]
}

export function updateHistoricalPeriod(periods, index, patch) {
  return periods.map((entry, i) => i === index ? { ...entry, ...patch } : entry)
}

export function removeHistoricalPeriod(periods, index) {
  return periods.filter((_, i) => i !== index)
}

function validRent(value) {
  return value !== '' && value !== null && value !== undefined && Number.isFinite(Number(value)) && Number(value) >= 0
}

/** Keep the tenant's move-in date independent of their changes in rent. */
export function buildRentSchedule({ stayStart, currentStartText, monthlyRent, historicalPeriods = [], retained = false }) {
  const currentStart = parseDisplayDate(currentStartText)
  if (!currentStart) throw new Error('Select a valid current rent start date.')
  const today = new Date()
  const todayLocal = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
  if (currentStart > todayLocal) throw new Error('The current rent cannot begin in the future.')
  if (!validRent(monthlyRent)) throw new Error('Enter a valid current monthly rent.')
  const current = { startDate: currentStart, endDate: null, monthlyRent: Number(monthlyRent) }
  const result = []
  for (let i = 0; i < historicalPeriods.length; i++) {
    const row = historicalPeriods[i]
    const from = parseDisplayDate(row.startDate)
    const until = parseDisplayDate(row.endDate)
    if (!from || !until) throw new Error(`Historical period ${i + 1}: select valid start and end dates.`)
    if (!validRent(row.monthlyRent)) throw new Error(`Historical period ${i + 1}: enter a valid monthly rent.`)
    if (until < from) throw new Error(`Historical period ${i + 1}: end date must be on or after its start date.`)
    result.push({ startDate: from, endDate: until, monthlyRent: Number(row.monthlyRent) })
  }
  // Sorting permits the administrator to add a forgotten period later.
  result.sort((a, b) => a.startDate.localeCompare(b.startDate))
  for (let i = 1; i < result.length; i++) {
    if (result[i].startDate <= result[i - 1].endDate) {
      throw new Error('Historical rent periods overlap. Dates may have gaps, but cannot overlap.')
    }
  }
  if (result.length && result.at(-1).endDate >= current.startDate) {
    throw new Error('Previous rent history overlaps the current rent start date. Gaps are allowed.')
  }
  result.push(current)
  if (!historicalPeriods.length && !retained) {
    // Keep the legacy simple-assignment behavior when no history was entered.
    return { stayStartDate: currentStart, rentPeriods: [] }
  }
  const originalStart = parseDisplayDate(stayStart)
  return {
    stayStartDate: retained ? originalStart : result[0].startDate,
    rentPeriods: result,
  }
}

export function validateRentHistory(state) {
  try { buildRentSchedule(state); return '' } catch (error) { return error.message }
}

export function rentPeriodsPayload(periods) {
  return (periods ?? []).map((p) => ({
    startDate: parseDisplayDate(p.startDate ?? p.startMonth),
    endDate: p.endDate || p.endMonth ? parseDisplayDate(p.endDate ?? p.endMonth) : null,
    monthlyRent: Number(p.monthlyRent),
  }))
}

// Compatibility wrappers for callers still using the monthly helper names.
export function appendRentPeriod(periods) { return appendHistoricalPeriod(periods) }
export function updateRentPeriod(periods, index, patch) { return updateHistoricalPeriod(periods, index, patch) }
export function removeRentPeriod(periods, index) { return removeHistoricalPeriod(periods, index) }
export function validateRentPeriods(periods, stayStart) {
  if (!periods?.length) return ''
  const form = scheduleToForm(periods, stayStart, periods.at(-1).monthlyRent)
  return validateRentHistory({ stayStart, ...form, retained: true })
}
