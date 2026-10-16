import { useCallback, useEffect, useRef, useState } from 'react'
import { NotificationSoundContext } from './notificationSoundStore.js'
const preferenceKey = 'finly-notification-sound-enabled'

function readPreference() {
  try { return localStorage.getItem(preferenceKey) === 'true' } catch { return false }
}

export function NotificationSoundProvider({ children }) {
  const [enabled, setEnabled] = useState(readPreference)
  const audioContextRef = useRef(null)

  const primeAudio = useCallback(() => {
    try {
      const AudioContextClass = window.AudioContext || window.webkitAudioContext
      if (!AudioContextClass) return
      const context = audioContextRef.current || new AudioContextClass()
      audioContextRef.current = context
      if (context.state === 'suspended') void context.resume().catch(() => {})
    } catch { /* Audio is an optional enhancement. */ }
  }, [])

  useEffect(() => {
    if (!enabled || typeof window === 'undefined') return undefined
    const unlock = () => primeAudio()
    window.addEventListener('pointerdown', unlock, { once: true, passive: true })
    window.addEventListener('keydown', unlock, { once: true })
    return () => {
      window.removeEventListener('pointerdown', unlock)
      window.removeEventListener('keydown', unlock)
    }
  }, [enabled, primeAudio])

  const setSoundEnabled = useCallback((nextValue) => {
    const next = Boolean(nextValue)
    setEnabled(next)
    try { localStorage.setItem(preferenceKey, String(next)) } catch { /* Sound still applies for this session. */ }
    if (next && typeof window !== 'undefined') primeAudio()
  }, [primeAudio])

  const playNotificationSound = useCallback(() => {
    const context = audioContextRef.current
    if (!enabled || !context || context.state !== 'running') return
    try {
      const now = context.currentTime
      ;[880, 1174.66].forEach((frequency, index) => {
        const start = now + index * 0.085
        const oscillator = context.createOscillator()
        const gain = context.createGain()
        oscillator.type = 'sine'
        oscillator.frequency.setValueAtTime(frequency, start)
        gain.gain.setValueAtTime(0.0001, start)
        gain.gain.exponentialRampToValueAtTime(0.035, start + 0.012)
        gain.gain.exponentialRampToValueAtTime(0.0001, start + 0.19)
        oscillator.connect(gain)
        gain.connect(context.destination)
        oscillator.start(start)
        oscillator.stop(start + 0.2)
      })
    } catch { /* Audio playback must never affect app functionality. */ }
  }, [enabled])

  return <NotificationSoundContext.Provider value={{ enabled, setSoundEnabled, playNotificationSound }}>{children}</NotificationSoundContext.Provider>
}
