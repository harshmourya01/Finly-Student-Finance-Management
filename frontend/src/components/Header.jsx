import { useLocation } from 'react-router-dom'
import { useTheme } from '../context/ThemeContext.jsx'

const titles = {
  '/': 'Dashboard',
  '/money': 'Money / Transactions',
  '/budget': 'Budget',
  '/savings-goals': 'Savings Goals',
  '/subscriptions': 'Subscriptions',
  '/analysis': 'Analysis',
  '/reports': 'Reports',
  '/settings': 'Settings',
}

export default function Header() {
  const { pathname } = useLocation()
  const { theme, toggleTheme } = useTheme()
  return (
    <header className="app-header">
      <span className="header-label">{titles[pathname] ?? 'Finly'}</span>
      <button className="theme-toggle" type="button" onClick={toggleTheme}
        aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}
        title={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}>
        {theme === 'dark'
          ? <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="4" /><path d="M12 2v2m0 16v2M4.93 4.93l1.42 1.42m11.3 11.3 1.42 1.42M2 12h2m16 0h2M4.93 19.07l1.42-1.42m11.3-11.3 1.42-1.42" /></svg>
          : <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20.2 15.6A8.6 8.6 0 0 1 8.4 3.8 8.7 8.7 0 1 0 20.2 15.6Z" /></svg>}
      </button>
    </header>
  )
}
