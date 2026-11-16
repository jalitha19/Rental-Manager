import { buildRentSchedule, scheduleToForm } from './rentPeriods.js'

export function getOccupants(rental) {
  if (!rental) return []
  if (rental.occupants?.length) return rental.occupants
  const legacy = []
  if (rental.tenant) legacy.push({ id: null, tenant: rental.tenant,
    startDate: rental.startDate, endDate: rental.endDate, monthlyRent: rental.monthlyRent })
  if (rental.tenant2) legacy.push({ id: null, tenant: rental.tenant2,
    startDate: rental.startDate, endDate: rental.endDate, monthlyRent: 0 })
  return legacy
}

export function getCurrentOccupants(rental) {
  return getOccupants(rental).filter((occupant) => !occupant.endDate)
}

export function occupantNames(rental) {
  return (rental?.status === 'ACTIVE' ? getCurrentOccupants(rental) : getOccupants(rental))
    .map((o) => o.tenant?.fullName).filter(Boolean).join(' & ')
}

export function emptyOccupant(startDate = '', monthlyRent = '') {
  return { tenantId: '', startDate, ...scheduleToForm([], startDate, monthlyRent),
    historicalPeriods: [], generateMissingPayments: false }
}

export function toOccupantPayload(occupants) {
  return occupants.filter((o) => o.tenantId).map((o) => {
    const retained = !!o.originalTenantId && String(o.originalTenantId) === String(o.tenantId)
    if (retained && !o.editRetainedSchedule) {
      // Don't even send rate edits for an occupant the user did not edit.
      return { tenantId: Number(o.tenantId), startDate: o.originalStartDate,
        monthlyRent: Number(o.originalMonthlyRent ?? o.monthlyRent) }
    }
    const schedule = buildRentSchedule({ stayStart: o.startDate,
      currentStartText: o.currentStartText, monthlyRent: o.monthlyRent,
      historicalPeriods: o.historicalPeriods ?? [], retained })
    return { tenantId: Number(o.tenantId), startDate: schedule.stayStartDate,
      monthlyRent: Number(o.monthlyRent),
      ...(schedule.rentPeriods.length ? {
        rentPeriods: schedule.rentPeriods,
        generateMissingPayments: !!o.generateMissingPayments,
      } : {}),
    }
  })
}

export function prefillCurrentOccupants(rental, fallbackRent = '', today = '') {
  const current = getCurrentOccupants(rental)
  return current.length ? current.map((occupant) => ({
    tenantId: String(occupant.tenant.id),
    originalTenantId: String(occupant.tenant.id),
    originalStartDate: occupant.startDate,
    originalMonthlyRent: occupant.monthlyRent ?? fallbackRent,
    startDate: occupant.startDate,
    monthlyRent: occupant.monthlyRent ?? fallbackRent,
    ...scheduleToForm(occupant.rentPeriods, occupant.startDate, occupant.monthlyRent ?? fallbackRent),
    existingRentPeriods: occupant.rentPeriods ?? [],
    editRetainedSchedule: false,
    generateMissingPayments: false,
  })) : [emptyOccupant(today, fallbackRent)]
}
