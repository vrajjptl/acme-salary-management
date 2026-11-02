import { useEffect, useState } from 'react'
import { Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import { api, setUnauthorizedHandler, warmup } from './api'
import { Shell } from './components'
import { AuditPage, DashboardPage, EmployeePage, LoginPage, NewEmployeePage, PeoplePage } from './pages'
import type { User } from './types'

export default function App() {
  const [user, setUser] = useState<User | null>(null)
  const [ready, setReady] = useState(false)
  const navigate = useNavigate()

  useEffect(() => {
    setUnauthorizedHandler(() => setUser(null))
    warmup()
      .then(() => api<User>('/api/auth/me'))
      .then((next) => setUser(next))
      .catch(() => setUser(null))
      .finally(() => setReady(true))
  }, [])

  if (!ready) return <div className="login-wrap"><p>Opening ACME Compensation…</p></div>
  if (!user) {
    return (
      <Routes>
        <Route path="/login" element={<LoginPage onSuccess={() => api<User>('/api/auth/me').then((next) => { setUser(next); navigate('/') })} />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    )
  }

  return (
    <Shell user={user} onLogout={() => setUser(null)}>
      <Routes>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/people" element={<PeoplePage />} />
        <Route path="/people/new" element={<NewEmployeePage />} />
        <Route path="/people/:id" element={<EmployeePage />} />
        <Route path="/audit" element={<AuditPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Shell>
  )
}
