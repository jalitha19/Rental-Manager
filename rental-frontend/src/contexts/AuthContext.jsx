import { createContext, useContext, useEffect, useState, useCallback } from 'react'
import { login as loginRequest, fetchCurrentUser } from '../services/authService'
import { setAuthToken } from '../services/api'

const STORAGE_KEY = 'rental_manager_token'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  const logout = useCallback(() => {
    localStorage.removeItem(STORAGE_KEY)
    setAuthToken(null)
    setUser(null)
  }, [])

  useEffect(() => {
    window.addEventListener('auth:unauthorized', logout)
    return () => window.removeEventListener('auth:unauthorized', logout)
  }, [logout])

  useEffect(() => {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (!stored) {
      setLoading(false)
      return
    }
    setAuthToken(stored)
    fetchCurrentUser()
      .then((u) => setUser(u))
      .catch(() => logout())
      .finally(() => setLoading(false))
  }, [logout])

  async function login(username, password) {
    const data = await loginRequest(username, password)
    localStorage.setItem(STORAGE_KEY, data.token)
    setAuthToken(data.token)
    setUser({ username: data.username })
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
