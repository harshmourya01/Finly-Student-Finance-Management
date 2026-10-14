import { useCallback, useEffect, useState } from 'react'
import { listSavingsGoals } from '../services/savingsGoals.js'

export default function useSavingsGoals() {
  const [goals, setGoals] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const reload = useCallback(async (signal) => {
    setLoading(true)
    setError('')
    try {
      const items = await listSavingsGoals(signal)
      setGoals(Array.isArray(items) ? items : [])
    } catch (err) {
      if (err.name !== 'AbortError') setError(err.message || 'Could not load savings goals.')
    } finally {
      if (!signal?.aborted) setLoading(false)
    }
  }, [])
  useEffect(() => {
    const controller = new AbortController()
    reload(controller.signal)
    return () => controller.abort()
  }, [reload])
  return { goals, setGoals, loading, error, reload }
}
