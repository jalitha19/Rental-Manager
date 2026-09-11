import api from './api'

export function listPayments(params = {}) {
  return api.get('/rent-payments', { params }).then((r) => r.data)
}

export function getPayment(id) {
  return api.get(`/rent-payments/${id}`).then((r) => r.data)
}

export function createPayment(payload) {
  return api.post('/rent-payments', payload).then((r) => r.data)
}

export function updatePayment(id, payload) {
  return api.put(`/rent-payments/${id}`, payload).then((r) => r.data)
}

export function deletePayment(id) {
  return api.delete(`/rent-payments/${id}`)
}

export function markPaymentPaid(id, payload) {
  return api.post(`/rent-payments/${id}/mark-paid`, payload).then((r) => r.data)
}

export function generateCurrentMonthPayments(toMonth = null) {
  const config = toMonth ? { params: { toMonth } } : {}
  return api.post('/rent-payments/generate-current-month', null, config).then((r) => r.data)
}

export function rentalPaymentHistory(rentalId) {
  return api.get(`/rentals/${rentalId}/payments`).then((r) => r.data)
}
