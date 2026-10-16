import { useEffect, useRef, useState } from 'react'
import useNotifications from '../hooks/useNotifications.js'
import '../styles/notifications.css'

export default function NotificationBell() {
  const [open, setOpen] = useState(false)
  const container = useRef(null)
  const { notifications, unreadCount, loading, busy, error, refresh, markRead, markAllRead } = useNotifications()

  useEffect(() => {
    const onPointerDown = (event) => {
      if (!container.current?.contains(event.target)) setOpen(false)
    }
    const onKeyDown = (event) => { if (event.key === 'Escape') setOpen(false) }
    document.addEventListener('pointerdown', onPointerDown)
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('pointerdown', onPointerDown)
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [])

  function toggle() {
    const next = !open
    setOpen(next)
    if (next) refresh({ quiet: true })
  }

  return (
    <div className="notification-bell-wrap" ref={container}>
      <button className="notification-bell" type="button" aria-label={unreadCount ? `Notifications, ${unreadCount} unread` : 'Notifications'}
        aria-expanded={open} aria-haspopup="dialog" onClick={toggle}>
        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M18 9a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4" /></svg>
        {unreadCount > 0 && <span className="notification-count">{unreadCount > 99 ? '99+' : unreadCount}</span>}
      </button>

      {open && <section className="notification-popover" role="dialog" aria-label="Notifications">
        <div className="notification-popover-header">
          <div><h2>Notifications</h2><p>{unreadCount ? `${unreadCount} unread` : 'You’re all caught up'}</p></div>
          <button type="button" className="notification-text-action" disabled={busy || unreadCount === 0} onClick={markAllRead}>Mark all as read</button>
        </div>
        {error && <div className="notification-error" role="alert">{error}<button type="button" onClick={() => refresh()}>Retry</button></div>}
        {loading ? <p className="notification-state" role="status">Loading notifications…</p>
          : notifications.length === 0 ? <p className="notification-state">No notifications yet.</p>
            : <ul className="notification-list">
              {notifications.map((item) => <li className={item.read ? 'notification-item is-read' : 'notification-item is-unread'} key={item.id}>
                <div className="notification-copy">
                  <div className="notification-item-title"><strong>{item.title}</strong>{!item.read && <i aria-label="Unread" />}</div>
                  <p>{item.message}</p>
                  <time dateTime={item.createdAt}>{formatTimestamp(item.createdAt)}</time>
                </div>
                {!item.read && <button type="button" className="notification-mark-read" disabled={busy} onClick={() => markRead(item.id)}>Mark read</button>}
              </li>)}
            </ul>}
      </section>}
    </div>
  )
}

function formatTimestamp(value) {
  if (!value) return ''
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '' : date.toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })
}
