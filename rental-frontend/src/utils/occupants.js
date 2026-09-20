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
  return getOccupants(rental)
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
    }))
}
