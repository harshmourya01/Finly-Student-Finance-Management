import { authenticatedApiRequest } from './api.js'
import { newNotificationRequestKey, refreshNotificationsAfterAction } from './notifications.js'

const pendingContributionKeys = new Map()

export const listSavingsGoals = (signal) => authenticatedApiRequest('/api/savings-goals', { signal })
export async function createSavingsGoal(goal) { const result = await authenticatedApiRequest('/api/savings-goals', { method: 'POST', body: JSON.stringify(goal) }); refreshNotificationsAfterAction(); return result }
export async function updateSavingsGoal(id, goal) { const result = await authenticatedApiRequest(`/api/savings-goals/${id}`, { method: 'PUT', body: JSON.stringify(goal) }); refreshNotificationsAfterAction(); return result }
export const deleteSavingsGoal = (id) => authenticatedApiRequest(`/api/savings-goals/${id}`, { method: 'DELETE' })
export async function addSavingsGoalMoney(id, amount, idempotencyKey) {
  const signature = `${id}:${amount}`
  const key = idempotencyKey || pendingContributionKeys.get(signature) || newNotificationRequestKey()
  pendingContributionKeys.set(signature, key)
  try {
    const result = await authenticatedApiRequest(`/api/savings-goals/${id}/add-money`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key },
      body: JSON.stringify({ amount }),
    })
    pendingContributionKeys.delete(signature)
    refreshNotificationsAfterAction()
    return result
  } catch (error) {
    if (error.status > 0 && error.status < 500) pendingContributionKeys.delete(signature)
    throw error
  }
}
