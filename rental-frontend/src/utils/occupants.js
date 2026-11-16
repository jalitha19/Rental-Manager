import { monthOf, rentPeriodsPayload } from './rentPeriods'

export function getOccupants(rental) {
  if (!rental) return []
  if (rental.occupants?.length) return rental.occupants
  const legacy = []
  if (rental.tenant) {
    legacy.push({
      id: null,
      tenant: rental.tenant,
      startDate: rental.startDate,
      endDate: rental.endDate,
      monthlyRent: rental.monthlyRent,
    })
  }
  if (rental.tenant2) {
    legacy.push({
      id: null,
      tenant: rental.tenant2,
      startDate: rental.startDate,
      endDate: rental.endDate,
      monthlyRent: 0,
    })
  }
  return legacy
}

export function occupantNames(rental) {
  return (rental?.status === 'ACTIVE' ? getCurrentOccupants(rental) : getOccupants(rental))
    .map((o) => o.tenant?.fullName)
    .filter(Boolean)
    .join(' & ')
}

export function emptyOccupant(startDate = '', monthlyRent = '') {
  return { tenantId: '', startDate, monthlyRent }
}

export function toOccupantPayload(occupants) {
  return occupants
    .filter((o) => o.tenantId)
    .map((o) => ({
      tenantId: Number(o.tenantId),
      startDate: o.startDate,
      monthlyRent: o.monthlyRent === '' || o.monthlyRent == null ? null : Number(o.monthlyRent),
      ...(o.rentPeriods?.length && (o.editRetainedSchedule || !o.originalTenantId || String(o.originalTenantId) !== String(o.tenantId))
        ? { rentPeriods: rentPeriodsPayload(o.rentPeriods),
            generateMissingPayments: !!o.generateMissingPayments } : {}),
    }))
}

// Only people whose stay has not ended are current occupants.
// Keep getOccupants() unfiltered for history and monthly reports.
export function getCurrentOccupants(rental) {
  return getOccupants(rental).filter((occupant) => !occupant.endDate)
}

export function prefillCurrentOccupants(rental, fallbackRent = '', today = '') {
  const current = getCurrentOccupants(rental)
  return current.length
    ? current.map((occupant) => ({
        tenantId: String(occupant.tenant.id),
        originalTenantId: String(occupant.tenant.id),
        originalStartDate: occupant.startDate,
        startDate: occupant.startDate,
        monthlyRent: occupant.monthlyRent ?? fallbackRent,
        rentPeriods: [],
        existingRentPeriods: (occupant.rentPeriods ?? []).map((p) => ({
          startMonth: monthOf(p.startMonth), endMonth: monthOf(p.endMonth), monthlyRent: p.monthlyRent,
        })),
        editRetainedSchedule: false,
      }))
    : [emptyOccupant(today, fallbackRent)]
}
