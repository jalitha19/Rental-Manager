import api from './api'

export function listTenants() {
  return api.get('/tenants').then((r) => r.data)
}

export function searchTenants(q) {
  return api.get('/tenants/search', { params: { q } }).then((r) => r.data)
}

export function getTenant(id) {
  return api.get(`/tenants/${id}`).then((r) => r.data)
}

export function createTenant(payload) {
  return api.post('/tenants', payload).then((r) => r.data)
}

export function updateTenant(id, payload) {
  return api.put(`/tenants/${id}`, payload).then((r) => r.data)
}

export function deleteTenant(id) {
  return api.delete(`/tenants/${id}`)
}

export function deleteTenantHistory(id) {
  return api.delete(`/tenants/${id}/history`)
}

export function addPhone(tenantId, payload) {
  return api.post(`/tenants/${tenantId}/phones`, payload).then((r) => r.data)
}

export function updatePhone(tenantId, phoneId, payload) {
  return api.put(`/tenants/${tenantId}/phones/${phoneId}`, payload).then((r) => r.data)
}

export function deletePhone(tenantId, phoneId) {
  return api.delete(`/tenants/${tenantId}/phones/${phoneId}`)
}
