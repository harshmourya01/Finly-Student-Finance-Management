import { useCallback, useEffect, useState } from 'react'
import { loadAnalysisData } from '../services/analysis.js'

export default function useAnalysis() {
  const [transactions, setTransactions] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [attempt, setAttempt] = useState(0)
  const reload = useCallback(async (signal) => {
    setLoading(true); setError('')
    try { setTransactions(await loadAnalysisData(signal)) }
    catch (reason) { if (!signal?.aborted) { setTransactions([]); setError(reason.message || 'Could not load transactions.') } }
    finally { if (!signal?.aborted) setLoading(false) }
  }, [])
  useEffect(() => { const controller = new AbortController(); reload(controller.signal); return () => controller.abort() }, [reload, attempt])
  return { transactions, loading, error, retry: () => setAttempt((value) => value + 1) }
}
