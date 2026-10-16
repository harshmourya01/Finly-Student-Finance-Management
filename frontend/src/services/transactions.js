import { authenticatedApiRequest } from './api.js'
import { newNotificationRequestKey, refreshNotificationsAfterAction } from './notifications.js'

const pendingTransactionKeys = new Map()

export function listTransactions(signal) {
  return authenticatedApiRequest('/api/transactions', { signal })
}

export function listCategories(signal) {
  return authenticatedApiRequest('/api/categories', { signal })
}

export function getBalanceSummary(signal) {
  return authenticatedApiRequest('/api/transactions/balance-summary', { signal })
}

export async function createTransaction(transaction, idempotencyKey) {
  const signature = JSON.stringify(transaction)
  const key = idempotencyKey || pendingTransactionKeys.get(signature) || newNotificationRequestKey()
  pendingTransactionKeys.set(signature, key)
  try {
    const result = await authenticatedApiRequest('/api/transactions', {
      method: 'POST',
      headers: { 'Idempotency-Key': key },
      body: signature,
    })
    pendingTransactionKeys.delete(signature)
    refreshNotificationsAfterAction()
    return result
  } catch (error) {
    if (error.status > 0 && error.status < 500) pendingTransactionKeys.delete(signature)
    throw error
  }
}

export async function updateTransaction(id, transaction) {
  const result = await authenticatedApiRequest(`/api/transactions/${id}`, {
    method: 'PUT',
    body: JSON.stringify(transaction),
  })
  refreshNotificationsAfterAction()
  return result
}

export function deleteTransaction(id) {
  return authenticatedApiRequest(`/api/transactions/${id}`, { method: 'DELETE' })
}
