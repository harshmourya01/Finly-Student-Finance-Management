import { authenticatedApiRequest } from './api.js'
import { listTransactions } from './transactions.js'

const asArray = (value, label) => {
  if (!Array.isArray(value)) throw new Error(`The server returned an invalid ${label} list.`)
  return value
}

export async function loadReportData(signal) {
  const results = await Promise.allSettled([
    listTransactions(signal),
    authenticatedApiRequest('/api/budgets', { signal }),
    authenticatedApiRequest('/api/savings-goals', { signal }),
    authenticatedApiRequest('/api/subscriptions', { signal }),
  ])
  if (signal?.aborted) throw new DOMException('Aborted', 'AbortError')
  const [transactions, budgets, goals, subscriptions] = results
  if (transactions.status === 'rejected') throw transactions.reason
  return {
    transactions: asArray(transactions.value, 'transaction'),
    budgets: budgets.status === 'fulfilled' ? asArray(budgets.value, 'budget') : [],
    goals: goals.status === 'fulfilled' ? asArray(goals.value, 'savings goal') : [],
    subscriptions: subscriptions.status === 'fulfilled' ? asArray(subscriptions.value, 'subscription') : [],
    partialError: [budgets, goals, subscriptions].some((result) => result.status === 'rejected')
      ? 'Some account data could not be loaded. Transaction reports remain available.' : '',
  }
}

export function downloadCsv(filename, rows) {
  const csv = rows.map((row) => row.map((value) => `"${String(value ?? '').replaceAll('"', '""')}"`).join(',')).join('\r\n')
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url; link.download = filename; link.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
