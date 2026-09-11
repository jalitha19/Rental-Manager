import api from './api'

export function listRentals(params = {}) {
  return api.get('/rentals', { params }).then((r) => r.data)
}

export function getRental(id) {
  return api.get(`/rentals/${id}`).then((r) => r.data)
}

export function createRental(payload) {
  return api.post('/rentals', payload).then((r) => r.data)
}

export function updateRental(id, payload) {
  return api.put(`/rentals/${id}`, payload).then((r) => r.data)
}

export function deleteRental(id) {
  return api.delete(`/rentals/${id}`)
}

export function endRental(id, payload) {
  return api.post(`/rentals/${id}/end`, payload).then((r) => r.data)
}

export function changeTenant(propertyId, payload) {
  return api.post(`/properties/${propertyId}/change-tenant`, payload).then((r) => r.data)
}

export function propertyRentalHistory(propertyId) {
  return api.get(`/properties/${propertyId}/rentals`).then((r) => r.data)
}

export function tenantRentalHistory(tenantId) {
  return api.get(`/tenants/${tenantId}/rentals`).then((r) => r.data)
}
