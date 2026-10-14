import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getCurrentUser, logout } from '../services/auth.js'
import { useTheme } from '../context/ThemeContext.jsx'

function initials(name = '') {
  const parts = name.trim().split(/\s+/).filter(Boolean)
  return (parts.length > 1 ? `${parts[0][0]}${parts.at(-1)[0]}` : (parts[0] || '').slice(0, 1)).toUpperCase()
}

export default function SettingsPage() {
  const navigate = useNavigate(); const [user, setUser] = useState(null); const [loading, setLoading] = useState(true); const [error, setError] = useState(''); const [attempt, setAttempt] = useState(0)
  const { theme, setTheme } = useTheme()
  useEffect(() => {
    let active = true; setLoading(true); setError('')
    getCurrentUser().then((account) => { if (active) setUser(account) }).catch((reason) => { if (active) setError(reason.message || 'Could not load your account information.') }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [attempt])
  function handleLogout() { logout(); navigate('/login', { replace: true }) }
  return <div className="module-page settings-page">
    <header className="page-heading"><div><p className="page-eyebrow">Account</p><h1>Settings</h1><p className="page-description">View your signed-in account information.</p></div></header>
    <section className="module-panel settings-profile" aria-labelledby="profile-heading"><div className="module-panel-heading"><div><h2 id="profile-heading">Profile information</h2><p>Details from your authenticated account</p></div></div>
      {loading ? <p className="settings-message" role="status">Loading account information…</p> : error ? <div className="settings-message" role="alert"><p>{error}</p><button type="button" className="module-secondary" onClick={() => setAttempt((value) => value + 1)}>Try again</button></div> : <div className="settings-identity"><div className="settings-avatar" aria-hidden="true">{initials(user?.fullName)}</div><div><span>Full name</span><strong>{user?.fullName}</strong><span>Email address</span><strong>{user?.email}</strong></div></div>}
    </section>
    <section className="module-panel settings-theme" aria-labelledby="theme-heading"><div><h2 id="theme-heading">Appearance</h2><p>Choose how Finly looks on this device.</p></div><label htmlFor="theme-choice">Theme<select id="theme-choice" value={theme} onChange={(event) => setTheme(event.target.value)}><option value="dark">Dark mode</option><option value="light">Light mode</option></select></label></section>
    <section className="module-panel settings-account"><div><h2>Account session</h2><p>Sign out of this account on this device.</p></div><button type="button" className="module-secondary settings-logout" onClick={handleLogout}>Log out</button></section>
  </div>
}
