/** The API stores rent changes by calendar month (the first day of the month).
 * The UI accepts full DD/MM/YYYY dates, but price changes are monthly, never daily. */
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

export function parseDisplayDate(value) {
  const match = String(value ?? '').trim().match(/^(\d{2})\/(\d{2})\/(\d{4})$/)
  if (!match) return null
  const [, dd, mm, yyyy] = match
  const year = Number(yyyy), month = Number(mm), day = Number(dd)
  const date = new Date(Date.UTC(year, month - 1, day))
  if (year < 1900 || date.getUTCFullYear() !== year || date.getUTCMonth() !== month - 1 || date.getUTCDate() !== day) return null
  return `${yyyy}-${mm}-${dd}`
}

export function endOfMonthIso(month) {
  if (!/^\d{4}-(0[1-9]|1[0-2])$/.test(month)) return ''
  const [year, value] = month.split('-').map(Number)
  const last = new Date(Date.UTC(year, value, 0))
  return `${month}-${String(last.getUTCDate()).padStart(2, '0')}`
}

export function initialRentPeriods(startDate, rent) {
  return [{ startMonth: monthOf(startDate), endMonth: null, monthlyRent: rent ?? '' }]
}

/** Convert backend monthly rate transitions to separate, fully dated history rows,
 * plus the ongoing price shown in the top (current) form. */
export function scheduleToForm(periods, originalStart, currentRent) {
  const rows = periods?.length ? periods : initialRentPeriods(originalStart, currentRent)
  const historicalPeriods = rows.slice(0, -1).map((entry) => ({
    startDate: displayDate(`${monthOf(entry.startMonth)}-01`),
    endDate: displayDate(endOfMonthIso(monthOf(entry.endMonth))),
    monthlyRent: String(entry.monthlyRent ?? ''),
  }))
  const latest = rows[rows.length - 1]
  return {
    currentStartText: displayDate(periods?.length ? `${monthOf(latest.startMonth)}-01` : originalStart),
    monthlyRent: String(latest.monthlyRent ?? currentRent ?? ''),
    historicalPeriods,
  }
}

export function appendHistoricalPeriod(periods) {
  if ((periods ?? []).length >= 99) return periods
  const previous = periods.at(-1)
  const after = previous ? parseDisplayDate(previous.endDate) : null
  return [...periods, {
    startDate: after ? displayDate(`${nextMonth(monthOf(after))}-01`) : '',
    endDate: '',
    monthlyRent: '',
  }]
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

/**
 * Build a contiguous rate schedule for the existing V7 table. The original
 * occupant start is independent from the current rent's effective month.
 * Existing retained tenants must keep their original start date. New tenants
 * with historical prices get their actual first historical date as stay start.
 * Throws rather than silently dropping invalid, overlapping, or partial dates.
 */
export function buildRentSchedule({ stayStart, currentStartText, monthlyRent, historicalPeriods = [], retained = false }) {
  const currentStart = parseDisplayDate(currentStartText)
  if (!currentStart) throw new Error('Enter the current rent start date as DD/MM/YYYY.')
  if (!validRent(monthlyRent)) throw new Error('Enter a valid current monthly rent.')
  if (!historicalPeriods.length && !retained) {
    return { stayStartDate: currentStart, rentPeriods: [] }
  }

  const now = new Date()
  const thisMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  if (monthOf(currentStart) > thisMonth) throw new Error('Future-effective rent periods are not supported yet.')
  if (!currentStart.endsWith('-01')) throw new Error('Current rent period must begin on the 1st of its month (DD/MM/YYYY).')

  const result = []
  let expected = retained ? monthOf(stayStart) : null
  for (let i = 0; i < historicalPeriods.length; i++) {
    const row = historicalPeriods[i]
    const from = parseDisplayDate(row.startDate)
    const until = parseDisplayDate(row.endDate)
    if (!from || !until) throw new Error(`Historical period ${i + 1}: enter valid DD/MM/YYYY start and end dates.`)
    if (!from.endsWith('-01')) throw new Error(`Historical period ${i + 1}: start date must be the 1st of its month.`)
    if (until !== endOfMonthIso(monthOf(until))) throw new Error(`Historical period ${i + 1}: end date must be the last day of its month.`)
    if (!validRent(row.monthlyRent)) throw new Error(`Historical period ${i + 1}: enter a valid monthly rent.`)
    const fromMonth = monthOf(from), untilMonth = monthOf(until)
    if (untilMonth < fromMonth) throw new Error(`Historical period ${i + 1}: end date is before start date.`)
    if (expected && fromMonth !== expected) throw new Error(`Historical period ${i + 1} must start in ${displayDate(`${expected}-01`).slice(3)}. No gaps or overlaps are allowed.`)
    if (!expected) expected = fromMonth
    result.push({ startMonth: `${fromMonth}-01`, endMonth: `${untilMonth}-01`, monthlyRent: Number(row.monthlyRent) })
    expected = nextMonth(untilMonth)
  }
  if (!expected) expected = monthOf(stayStart)
  if (monthOf(currentStart) !== expected) {
    throw new Error(`Current rent must start in ${displayDate(`${expected}-01`).slice(3)}. Adjust historical end dates to leave no gaps or overlaps.`)
  }
  result.push({ startMonth: `${monthOf(currentStart)}-01`, endMonth: null, monthlyRent: Number(monthlyRent) })
  return {
    stayStartDate: retained ? stayStart : parseDisplayDate(historicalPeriods[0].startDate),
    rentPeriods: result,
  }
}

export function validateRentHistory(state) {
  try { buildRentSchedule(state); return '' } catch (error) { return error.message }
}

/** Legacy backend wire format, retained for API compatibility. */
export function rentPeriodsPayload(periods) {
  return (periods ?? []).map((p) => ({
    startMonth: `${monthOf(p.startMonth)}-01`,
    endMonth: p.endMonth ? `${monthOf(p.endMonth)}-01` : null,
    monthlyRent: Number(p.monthlyRent),
  }))
}

// Backward-compatible old period helpers used only by external callers/tests.
export function appendRentPeriod(periods) {
  if (!periods.length) return initialRentPeriods('', '')
  const result = periods.map((p) => ({ ...p }))
  const last = result.at(-1)
  if (!last.endMonth) last.endMonth = last.startMonth
  result.push({ startMonth: nextMonth(monthOf(last.endMonth)), endMonth: null, monthlyRent: last.monthlyRent })
  return result
}
export function updateRentPeriod(periods, index, patch) {
  return periods.map((p, i) => i === index ? { ...p, ...patch } : { ...p })
}
export function removeRentPeriod(periods, index) {
  return periods.filter((_, i) => i !== index)
}
export function validateRentPeriods(periods, stayStart) {
  if (!periods?.length) return ''
  try {
    const form = scheduleToForm(periods, stayStart, periods.at(-1).monthlyRent)
    buildRentSchedule({ stayStart, ...form, retained: true })
    return ''
  } catch (error) { return error.message }
}
