import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './contexts/AuthContext'
import { ToastProvider } from './contexts/ToastContext'
import ProtectedRoute from './components/ProtectedRoute'
import AppLayout from './layouts/AppLayout'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Houses from './pages/Houses'
import Rooms from './pages/Rooms'
import Tenants from './pages/Tenants'
import TenantDetail from './pages/TenantDetail'
import Rentals from './pages/Rentals'
import Payments from './pages/Payments'
import Settings from './pages/Settings'
import Search from './pages/Search'

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ToastProvider>
          <Routes>
            <Route path="/login" element={<Login />} />

            <Route
              path="/"
              element={
                <ProtectedRoute>
                  <AppLayout />
                </ProtectedRoute>
              }
            >
              <Route index element={<Dashboard />} />
              <Route path="houses" element={<Houses />} />
              <Route path="rooms" element={<Rooms />} />
              <Route path="tenants" element={<Tenants />} />
              <Route path="tenants/:id" element={<TenantDetail />} />
              <Route path="rentals" element={<Rentals />} />
              <Route path="payments" element={<Payments />} />
              <Route path="settings" element={<Settings />} />
              <Route path="search" element={<Search />} />
            </Route>

            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  )
}
