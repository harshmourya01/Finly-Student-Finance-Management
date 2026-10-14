import { useCallback, useEffect, useState } from 'react'
import { listSubscriptions } from '../services/subscriptions.js'

export default function useSubscriptions() {
  const [subscriptions, setSubscriptions] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const reload = useCallback(async (signal) => {
    setLoading(true)
    setError('')
    try {
      const items = await listSubscriptions(signal)
      setSubscriptions(Array.isArray(items) ? items : [])
    } catch (err) {
      if (err.name !== 'AbortError') setError(err.message || 'Could not load subscriptions.')
    } finally {
      if (!signal?.aborted) setLoading(false)
    }
  }, [])
  useEffect(() => {
    const controller = new AbortController()
    reload(controller.signal)
    return () => controller.abort()
  }, [reload])
  return { subscriptions, setSubscriptions, loading, error, reload }
}
