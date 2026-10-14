import { useCallback, useEffect, useState } from 'react'
import { listBudgets, listExpenseCategories } from '../services/budgets.js'

export function currentMonthKey(date = new Date()) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
}

export default function useBudgets(month) {
  const [budgets, setBudgets] = useState([])
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const reload = useCallback(async (signal) => {
    setLoading(true)
    setError('')
    try {
      const [items, categoryItems] = await Promise.all([listBudgets(month, signal), listExpenseCategories(signal)])
      setBudgets(Array.isArray(items) ? items : [])
      setCategories(Array.isArray(categoryItems) ? categoryItems : [])
    } catch (err) {
      if (err.name !== 'AbortError') setError(err.message || 'Could not load budgets.')
    } finally {
      if (!signal?.aborted) setLoading(false)
    }
  }, [month])
  useEffect(() => {
    const controller = new AbortController()
    reload(controller.signal)
    return () => controller.abort()
  }, [reload])
  return { budgets, setBudgets, categories, loading, error, reload }
}
