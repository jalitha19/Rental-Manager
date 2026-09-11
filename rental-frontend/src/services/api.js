import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
})

// Token lives in memory (via AuthContext) rather than localStorage, so it's
// set on the instance directly by AuthContext when the user logs in.
let authToken = null

export function setAuthToken(token) {
  authToken = token
}

api.interceptors.request.use((config) => {
  if (authToken) {
    config.headers.Authorization = `Bearer ${authToken}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const url = error.config?.url ?? ''
    const isAuthAttempt = url.includes('/auth/login') || url.includes('/auth/change-password')
    if (error.response?.status === 401 && !isAuthAttempt) {
      window.dispatchEvent(new CustomEvent('auth:unauthorized'))
    }
    return Promise.reject(error)
  }
)

export function getErrorMessage(error, fallback = 'Something went wrong. Please try again.') {
  const data = error?.response?.data
  if (data?.details?.length) return data.details.join('. ')
  if (data?.message) return data.message
  if (error?.message) return error.message
  return fallback
}

export default api
