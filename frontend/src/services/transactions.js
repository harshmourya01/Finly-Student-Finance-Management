import { authenticatedApiRequest } from './api.js'

export function listTransactions(signal) {
  return authenticatedApiRequest('/api/transactions', { signal })
}

export function listCategories(signal) {
  return authenticatedApiRequest('/api/categories', { signal })
}

export function getBalanceSummary(signal) {
  return authenticatedApiRequest('/api/transactions/balance-summary', { signal })
}

export function createTransaction(transaction) {
  return authenticatedApiRequest('/api/transactions', {
    method: 'POST',
    body: JSON.stringify(transaction),
  })
}

export function updateTransaction(id, transaction) {
  return authenticatedApiRequest(`/api/transactions/${id}`, {
    method: 'PUT',
    body: JSON.stringify(transaction),
  })
}

export function deleteTransaction(id) {
  return authenticatedApiRequest(`/api/transactions/${id}`, { method: 'DELETE' })
}
