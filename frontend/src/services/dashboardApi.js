import { authenticatedApiRequest } from './api.js'

export async function fetchDashboardResource(path, signal) {
  return authenticatedApiRequest(path, { signal })
}

export function currentMonthKey(date = new Date()) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
}
