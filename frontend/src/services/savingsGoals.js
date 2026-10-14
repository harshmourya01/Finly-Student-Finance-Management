import { authenticatedApiRequest } from './api.js'

export const listSavingsGoals = (signal) => authenticatedApiRequest('/api/savings-goals', { signal })
export const createSavingsGoal = (goal) => authenticatedApiRequest('/api/savings-goals', { method: 'POST', body: JSON.stringify(goal) })
export const updateSavingsGoal = (id, goal) => authenticatedApiRequest(`/api/savings-goals/${id}`, { method: 'PUT', body: JSON.stringify(goal) })
export const deleteSavingsGoal = (id) => authenticatedApiRequest(`/api/savings-goals/${id}`, { method: 'DELETE' })
export const addSavingsGoalMoney = (id, amount) => authenticatedApiRequest(`/api/savings-goals/${id}/add-money`, { method: 'POST', body: JSON.stringify({ amount }) })
