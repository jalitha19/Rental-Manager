import api from './api'

export function listProperties(type) {
  return api.get('/properties', { params: type ? { type } : {} }).then((r) => r.data)
}

export function searchProperties(q) {
  return api.get('/properties/search', { params: { q } }).then((r) => r.data)
}

export function getProperty(id) {
  return api.get(`/properties/${id}`).then((r) => r.data)
}

export function createProperty(payload) {
  return api.post('/properties', payload).then((r) => r.data)
}

export function updateProperty(id, payload) {
  return api.put(`/properties/${id}`, payload).then((r) => r.data)
}

export function deleteProperty(id) {
  return api.delete(`/properties/${id}`)
}
