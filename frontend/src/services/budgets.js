import { authenticatedApiRequest } from './api.js'

export const listBudgets = (month, signal) => authenticatedApiRequest(`/api/budgets?month=${encodeURIComponent(month)}`, { signal })
export const createBudget = (budget) => authenticatedApiRequest('/api/budgets', { method: 'POST', body: JSON.stringify(budget) })
export const updateBudget = (id, budget) => authenticatedApiRequest(`/api/budgets/${id}`, { method: 'PUT', body: JSON.stringify(budget) })
export const deleteBudget = (id) => authenticatedApiRequest(`/api/budgets/${id}`, { method: 'DELETE' })
export const listExpenseCategories = (signal) => authenticatedApiRequest('/api/categories?type=EXPENSE', { signal })
