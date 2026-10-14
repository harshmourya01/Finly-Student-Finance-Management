import { NavLink, useNavigate } from 'react-router-dom'
import { logout } from '../services/auth.js'

const navigation = [
  { to: '/', label: 'Dashboard', symbol: '⌂', end: true },
  { to: '/money', label: 'Money / Transactions', symbol: '▣' },
  { to: '/budget', label: 'Budget', symbol: '▤' },
  { to: '/savings-goals', label: 'Savings Goals', symbol: '◎' },
  { to: '/subscriptions', label: 'Subscriptions', symbol: '↻' },
  { to: '/analysis', label: 'Analysis', symbol: '⌁' },
  { to: '/reports', label: 'Reports', symbol: '▧' },
  { to: '/settings', label: 'Settings', symbol: '⚙' },
]

export default function Sidebar() {
  const navigate = useNavigate()
  const handleLogout = () => { logout(); navigate('/login') }
  return (
    <aside className="sidebar">
      <NavLink className="brand" to="/" aria-label="Finly dashboard">
        <span className="brand-mark" aria-hidden="true">₹</span>
        <span>Finly</span>
      </NavLink>
      <p className="sidebar-caption">Workspace</p>
      <nav className="sidebar-nav" aria-label="Main navigation">
        {navigation.map(({ to, label, symbol, end }) => (
          <NavLink key={to} to={to} end={end} className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>
            <span className="nav-symbol" aria-hidden="true">{symbol}</span>
            <span>{label}</span>
          </NavLink>
        ))}
        <button type="button" className="nav-link sidebar-logout-mobile" onClick={handleLogout}>
          <span className="nav-symbol" aria-hidden="true">↪</span><span>Logout</span>
        </button>
      </nav>
      <div className="sidebar-footer">
        <button type="button" className="sidebar-logout" onClick={handleLogout}>↪ <span>Logout</span></button>
        <p>Student Finance<br />Personal overview</p>
      </div>
    </aside>
  )
}
