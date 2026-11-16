/** Month controls use YYYY-MM; the backend stores dates on the first of each month. */
export const monthOf = (date) => String(date ?? '').slice(0, 7)

export function nextMonth(month) {
  if (!/^\d{4}-\d{2}$/.test(month)) return ''
  const [year, value] = month.split('-').map(Number)
  const next = new Date(Date.UTC(year, value, 1))
  return `${next.getUTCFullYear()}-${String(next.getUTCMonth() + 1).padStart(2, '0')}`
}

export function initialRentPeriods(startDate, rent) {
  return [{ startMonth: monthOf(startDate), endMonth: '', monthlyRent: rent ?? '' }]
}

export function appendRentPeriod(periods) {
  if (!periods.length) return initialRentPeriods('', '')
  const result = periods.map((row) => ({ ...row }))
  const last = result[result.length - 1]
  if (!last.endMonth) last.endMonth = last.startMonth
  result.push({ startMonth: nextMonth(last.endMonth), endMonth: '', monthlyRent: last.monthlyRent })
  return result
}

export function updateRentPeriod(periods, index, patch) {
  const result = periods.map((row, i) => i === index ? { ...row, ...patch } : { ...row })
  if (Object.hasOwn(patch, 'endMonth') && index + 1 < result.length) {
    result[index + 1].startMonth = nextMonth(patch.endMonth)
  }
  return result
}

export function removeRentPeriod(periods, index) {
  const result = periods.filter((_, i) => i !== index).map((p) => ({ ...p }))
  if (!result.length) return result
  if (index === 0) return result // the parent controls the tenant start date
  if (index < result.length) result[index].startMonth = nextMonth(result[index - 1].endMonth)
  result[result.length - 1].endMonth = ''
  return result
}

/** Return an error message if months overlap or leave gaps. */
export function validateRentPeriods(periods, stayStart) {
  if (!periods?.length) return ''
  let expected = monthOf(stayStart)
  for (let i = 0; i < periods.length; i++) {
    const p = periods[i]
    if (!p.startMonth || p.startMonth !== expected) return `Period ${i + 1} must start in ${expected || 'the tenant start month'}.`
    if (p.monthlyRent === '' || p.monthlyRent == null || Number(p.monthlyRent) < 0) return `Enter a valid rent for period ${i + 1}.`
    if (i === periods.length - 1) {
      if (p.endMonth) return 'The last rent period must continue to Present.'
    } else {
      if (!p.endMonth || p.endMonth < p.startMonth) return `Enter an end month for period ${i + 1}.`
      expected = nextMonth(p.endMonth)
    }
  }
  return ''
}

export function rentPeriodsPayload(periods) {
  return (periods ?? []).map((p) => ({
    startMonth: `${p.startMonth}-01`,
    endMonth: p.endMonth ? `${p.endMonth}-01` : null,
    monthlyRent: Number(p.monthlyRent),
  }))
}
