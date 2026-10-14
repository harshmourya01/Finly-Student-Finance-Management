import { useCallback, useEffect, useState } from 'react'
import { getBalanceSummary, listCategories, listTransactions } from '../services/transactions.js'

export default function useTransactions() {
  const [transactions, setTransactions] = useState([])
  const [categories, setCategories] = useState([])
  const [balance, setBalance] = useState(null)
  const [loading, setLoading] = useState(true)
  const [errors, setErrors] = useState({})

  const reload = useCallback(async ({ signal, showLoading = true } = {}) => {
    if (showLoading) setLoading(true)
    const results = await Promise.allSettled([
      listTransactions(signal), listCategories(signal), getBalanceSummary(signal),
    ])
    if (signal?.aborted) return

    const [transactionResult, categoryResult, balanceResult] = results
    const nextErrors = {}
    if (transactionResult.status === 'fulfilled' && Array.isArray(transactionResult.value)) {
      setTransactions(transactionResult.value)
    } else {
      setTransactions([])
      nextErrors.transactions = transactionResult.status === 'rejected'
        ? transactionResult.reason.message : 'The server returned an invalid transaction list.'
    }
    if (categoryResult.status === 'fulfilled' && Array.isArray(categoryResult.value)) {
      setCategories(categoryResult.value)
    } else {
      setCategories([])
      nextErrors.categories = categoryResult.status === 'rejected'
        ? categoryResult.reason.message : 'The server returned an invalid category list.'
    }
    if (balanceResult.status === 'fulfilled' && balanceResult.value && 'spendableBalance' in balanceResult.value) {
      setBalance(balanceResult.value)
    } else {
      setBalance(null)
      nextErrors.balance = balanceResult.status === 'rejected'
        ? balanceResult.reason.message : 'The server returned an invalid balance summary.'
    }
    setErrors(nextErrors)
    setLoading(false)
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    reload({ signal: controller.signal }).catch((error) => {
      if (error.name !== 'AbortError') {
        setErrors({ transactions: error.message, categories: error.message, balance: error.message })
        setLoading(false)
      }
    })
    return () => controller.abort()
  }, [reload])

  return { transactions, setTransactions, categories, balance, loading, errors, reload }
}
