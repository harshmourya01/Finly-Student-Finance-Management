import { useContext } from 'react'
import { NotificationSoundContext } from '../context/notificationSoundStore.js'

export function useNotificationSound() {
  const value = useContext(NotificationSoundContext)
  if (!value) throw new Error('useNotificationSound must be used within NotificationSoundProvider')
  return value
}
