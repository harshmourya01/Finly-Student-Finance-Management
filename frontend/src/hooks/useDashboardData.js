import { useCallback, useEffect, useState } from 'react'
import { currentMonthKey, fetchDashboardResource } from '../services/dashboardApi.js'

const initialData = { transactions: [], budgets: [], goals: [], subscriptions: [], balance: null }

export default function useDashboardData() {
  const [data, setData] = useState(initialData)
  const [errors, setErrors] = useState({})
  const [loading, setLoading] = useState(true)
  const refresh = useCallback(async (signal) => {
    setLoading(true)
    const month = currentMonthKey()
    const resources = [
      ['transactions', '/api/transactions'],
      ['balance', '/api/transactions/balance-summary'],
      ['budgets', `/api/budgets?month=${month}`],
      ['goals', '/api/savings-goals'],
      ['subscriptions', '/api/subscriptions'],
    ]
    const results = await Promise.all(resources.map(async ([key, path]) => {
      try {
        const value = await fetchDashboardResource(path, signal)
        if (key !== 'balance' && !Array.isArray(value)) throw new Error('The server returned unexpected dashboard data.')
        return [key, value, null]
      } catch (error) {
        if (error.name === 'AbortError') throw error
        return [key, null, error]
      }
    }))
    if (signal?.aborted) return
    const nextData = { ...initialData }
    const nextErrors = {}
    results.forEach(([key, value, error]) => {
      if (error) {
        nextErrors[key] = error.message
        if (error.code === 'MISSING_TOKEN' || error.code === 'UNAUTHORIZED') nextErrors.dashboard = error.message
      }
      else nextData[key] = value
    })
    setData(nextData)
    setErrors(nextErrors)
    setLoading(false)
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    refresh(controller.signal).catch((error) => {
      if (error.name !== 'AbortError') {
        setErrors({ dashboard: error.message })
        setLoading(false)
      }
    })
    return () => controller.abort()
  }, [refresh])

  return { ...data, errors, loading, refresh: () => refresh() }
}
