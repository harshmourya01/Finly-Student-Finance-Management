import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { logout } from '../../services/auth.js'
import { getNotifications } from '../../utils/dashboard.js'

const periods = [
  ['month', 'This Month'], ['last', 'Last Month'], ['six', 'Last 6 months'], ['year', 'This Year'],
]

export default function DashboardToolbar({ data, errors, period, setPeriod }) {
  const [openMenu, setOpenMenu] = useState(null)
  const [notificationsOpen, setNotificationsOpen] = useState(false)
  const root = useRef(null)
  const navigate = useNavigate()
  const notices = getNotifications(data)
  const selectedLabel = periods.find(([key]) => key === period)?.[1] ?? periods[0][1]

  useEffect(() => {
    const onPointerDown = (event) => {
      if (!root.current?.contains(event.target)) {
        setOpenMenu(null)
        setNotificationsOpen(false)
      }
    }
    const onKeyDown = (event) => { if (event.key === 'Escape') { setOpenMenu(null); setNotificationsOpen(false) } }
    document.addEventListener('pointerdown', onPointerDown)
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('pointerdown', onPointerDown)
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [])

  const menu = (key, label, choices, onSelect, className = '') => (
    <div className={`dashboard-menu-wrap ${className}`}>
      <button className="dashboard-control" type="button" aria-haspopup="menu" aria-expanded={openMenu === key} onClick={() => setOpenMenu(openMenu === key ? null : key)}>
        <span>{label}</span><span aria-hidden="true">⌄</span>
      </button>
      {openMenu === key && <div className="dashboard-menu" role="menu">
        {choices.map(([value, text]) => <button key={value} type="button" role="menuitem" onClick={() => { onSelect(value); setOpenMenu(null) }}>{text}</button>)}
      </div>}
    </div>
  )

  return (
    <div className="dashboard-toolbar" ref={root}>
      {menu('period', selectedLabel, periods, setPeriod, 'period-menu-wrap')}
      <div className="dashboard-menu-wrap">
        <button className="dashboard-icon-button" type="button" aria-label="Notifications" aria-expanded={notificationsOpen} onClick={() => { setNotificationsOpen(!notificationsOpen); setOpenMenu(null) }}>
          <span aria-hidden="true">♧</span>{notices.length > 0 && <i className="notification-dot" />}
        </button>
        {notificationsOpen && <section className="notification-panel" role="status" aria-live="polite">
          <h3>Notifications</h3>
          {Object.values(errors).some(Boolean)
            ? <p>Some account data could not be loaded.</p>
            : notices.length
              ? <ul>{notices.map((notice, index) => <li key={`${notice.title}-${index}`}><strong>{notice.title}</strong>{notice.text}</li>)}</ul>
              : <p>No notifications right now.</p>}
        </section>}
      </div>
      <button className="dashboard-avatar" type="button" aria-label="Logout" title="Logout" onClick={() => {
        logout()
        navigate('/login')
      }}>
        {getInitials(sessionStorage.getItem('currentUser'))}
      </button>
    </div>
  )
}

function getInitials(rawUser) {
  try {
    const name = JSON.parse(rawUser || 'null')?.fullName?.trim()
    return name ? name.split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase() : '·'
  } catch {
    return '·'
  }
}
