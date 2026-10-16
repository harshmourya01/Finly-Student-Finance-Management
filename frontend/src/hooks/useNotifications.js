import { useCallback, useEffect, useState } from 'react'
import {
  getUnreadNotificationCount,
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead,
} from '../services/notifications.js'

export default function useNotifications() {
  const [notifications, setNotifications] = useState([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  const refresh = useCallback(async ({ quiet = false } = {}) => {
    if (!quiet) setLoading(true)
    try {
      const [items, count] = await Promise.all([listNotifications(), getUnreadNotificationCount()])
      setNotifications(Array.isArray(items) ? items : [])
      setUnreadCount(Number(count?.unreadCount) || 0)
      setError('')
    } catch (err) {
      setError(err.message || 'Notifications could not be loaded.')
    } finally {
      if (!quiet) setLoading(false)
    }
  }, [])

  useEffect(() => {
    refresh()
    const onUpdate = () => refresh({ quiet: true })
    const onVisible = () => { if (document.visibilityState === 'visible') refresh({ quiet: true }) }
    window.addEventListener('finly:notifications-updated', onUpdate)
    window.addEventListener('focus', onVisible)
    const interval = window.setInterval(() => refresh({ quiet: true }), 60_000)
    return () => {
      window.removeEventListener('finly:notifications-updated', onUpdate)
      window.removeEventListener('focus', onVisible)
      window.clearInterval(interval)
    }
  }, [refresh])

  const markRead = useCallback(async (id) => {
    setBusy(true)
    setError('')
    try {
      await markNotificationRead(id)
      await refresh({ quiet: true })
    } catch (err) {
      setError(err.message || 'Could not mark this notification as read.')
    } finally {
      setBusy(false)
    }
  }, [refresh])

  const markAllRead = useCallback(async () => {
    setBusy(true)
    setError('')
    try {
      await markAllNotificationsRead()
      await refresh({ quiet: true })
    } catch (err) {
      setError(err.message || 'Could not mark notifications as read.')
    } finally {
      setBusy(false)
    }
  }, [refresh])

  return { notifications, unreadCount, loading, busy, error, refresh, markRead, markAllRead }
}
