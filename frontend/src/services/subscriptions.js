import { authenticatedApiRequest } from './api.js'

export const listSubscriptions = (signal) => authenticatedApiRequest('/api/subscriptions', { signal })
export const createSubscription = (subscription) => authenticatedApiRequest('/api/subscriptions', { method: 'POST', body: JSON.stringify(subscription) })
export const updateSubscription = (id, subscription) => authenticatedApiRequest(`/api/subscriptions/${id}`, { method: 'PUT', body: JSON.stringify(subscription) })
export const deleteSubscription = (id) => authenticatedApiRequest(`/api/subscriptions/${id}`, { method: 'DELETE' })
export const markSubscriptionPaid = (id) => authenticatedApiRequest(`/api/subscriptions/${id}/pay`, { method: 'POST' })
