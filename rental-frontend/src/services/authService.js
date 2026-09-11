import api from './api'

export async function login(username, password) {
  const { data } = await api.post('/auth/login', { username, password })
  return data // { token, username, expiresInMs }
}

export async function fetchCurrentUser() {
  const { data } = await api.get('/auth/me')
  return data // { username }
}

export async function changePassword(currentPassword, newPassword) {
  await api.post('/auth/change-password', { currentPassword, newPassword })
}
