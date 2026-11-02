import { useState, type ReactNode } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { api } from './api'
import type { User } from './types'
import { money, titleCase } from './format'

export function Shell({ user, onLogout, children }: { user: User; onLogout: () => void; children: ReactNode }) {
  const navigate = useNavigate()
  const [signingOut, setSigningOut] = useState(false)
  const [logoutError, setLogoutError] = useState<string | null>(null)
  async function logout() {
    setSigningOut(true)
    setLogoutError(null)
    try {
      await api('/api/auth/logout', { method: 'POST' })
      onLogout()
      navigate('/login')
    } catch {
      setLogoutError('Sign out did not complete.')
      setSigningOut(false)
    }
  }
  const initials = user.username
    .split(/[.\s_-]+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? '')
    .join('')
  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="mark">
          <strong>ACME</strong>
          <span>Compensation</span>
        </div>
        <nav className="nav" aria-label="Workspace">
          <NavLink to="/" end>How we pay</NavLink>
          <NavLink to="/people">People</NavLink>
          <NavLink to="/audit">Audit</NavLink>
        </nav>
      </aside>
      <header className="topbar">
        <div className="topbar-title">HR workspace</div>
        <div className="account-block">
          <div className="account">
            <span className="avatar" aria-hidden="true">{initials || 'HR'}</span>
            <span className="account-name">
              <strong>{user.username}</strong>
              <span>{titleCase(user.role)}</span>
            </span>
            <button className="sign-out" type="button" onClick={logout} disabled={signingOut}>
              {signingOut ? 'Signing out…' : 'Sign out'}
            </button>
          </div>
          {logoutError && <p className="logout-error" role="alert">{logoutError}</p>}
        </div>
      </header>
      <main className="main">{children}</main>
    </div>
  )
}

export function BandMeter({ min, max, value, currency }: { min: string; max: string; value: string; currency: string }) {
  const minN = Number(min)
  const maxN = Number(max)
  const current = Number(value)
  const span = maxN - minN || 1
  const raw = ((current - minN) / span) * 100
  const left = Math.min(100, Math.max(0, raw))
  return (
    <div className="meter">
      <div className="track" aria-hidden="true">
        <i className="marker" style={{ left: `calc(${left}% - 2px)` }} />
      </div>
      <div className="meter-scale">
        <span>{money(min, currency)} min</span>
        <span>{money(max, currency)} max</span>
      </div>
    </div>
  )
}

export function Banner({ error }: { error: string | string[] | null }) {
  if (!error) return null
  const lines = Array.isArray(error) ? error : [error]
  return (
    <div className="error" role="alert">
      {lines.map((line) => <div key={line}>{line}</div>)}
    </div>
  )
}
