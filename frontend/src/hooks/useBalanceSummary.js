import { useCallback, useEffect, useState } from 'react'
import { getBalanceSummary } from '../services/transactions.js'

export default function useBalanceSummary() {
  const [balance, setBalance] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const reload = useCallback(async (signal) => {
    setError('')
    try {
      setBalance(await getBalanceSummary(signal))
    } catch (err) {
      if (err.name !== 'AbortError') setError(err.message || 'Available to Spend could not be loaded.')
    } finally {
      if (!signal?.aborted) setLoading(false)
    }
  }, [])
  useEffect(() => {
    const controller = new AbortController()
    reload(controller.signal)
    return () => controller.abort()
  }, [reload])
  return { balance, loading, error, reload }
}
