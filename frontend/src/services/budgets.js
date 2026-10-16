import { authenticatedApiRequest } from './api.js'
import { refreshNotificationsAfterAction } from './notifications.js'

export const listBudgets = (month, signal) => authenticatedApiRequest(`/api/budgets?month=${encodeURIComponent(month)}`, { signal })
export async function createBudget(budget) { const result = await authenticatedApiRequest('/api/budgets', { method: 'POST', body: JSON.stringify(budget) }); refreshNotificationsAfterAction(); return result }
export async function updateBudget(id, budget) { const result = await authenticatedApiRequest(`/api/budgets/${id}`, { method: 'PUT', body: JSON.stringify(budget) }); refreshNotificationsAfterAction(); return result }
export const deleteBudget = (id) => authenticatedApiRequest(`/api/budgets/${id}`, { method: 'DELETE' })
export const listExpenseCategories = (signal) => authenticatedApiRequest('/api/categories?type=EXPENSE', { signal })
