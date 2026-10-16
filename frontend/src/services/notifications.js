import { authenticatedApiRequest } from './api.js'

export const listNotifications = () => authenticatedApiRequest('/api/notifications?limit=10')
export const getUnreadNotificationCount = () => authenticatedApiRequest('/api/notifications/unread-count')
export const markNotificationRead = (id) => authenticatedApiRequest(`/api/notifications/${id}/read`, { method: 'PATCH' })
export const markAllNotificationsRead = () => authenticatedApiRequest('/api/notifications/read-all', { method: 'PATCH' })
export const deleteNotification = (id) => authenticatedApiRequest(`/api/notifications/${id}`, { method: 'DELETE' })

export function refreshNotificationsAfterAction() {
  if (typeof window !== 'undefined') window.dispatchEvent(new Event('finly:notifications-updated'))
}

export function newNotificationRequestKey() {
  return globalThis.crypto?.randomUUID?.() ?? `finly-${Date.now()}-${Math.random().toString(36).slice(2)}`
}
