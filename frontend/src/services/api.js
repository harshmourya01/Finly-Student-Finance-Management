const configuredBase = import.meta.env?.VITE_API_BASE_URL ?? ''
export const API_BASE_URL = configuredBase.replace(/\/+$/, '')

export class ApiError extends Error {
  constructor(message, status = 0, code = 'API_ERROR') {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

export function getAuthToken() {
  return sessionStorage.getItem('authToken')
}

export function clearAuthSession() {
  sessionStorage.removeItem('authToken')
  sessionStorage.removeItem('currentUser')
}

export function storeAuthSession(token, user) {
  sessionStorage.setItem('authToken', token)
  if (user) sessionStorage.setItem('currentUser', JSON.stringify(user))
  else sessionStorage.removeItem('currentUser')
}

export async function apiRequest(path, { authenticated = false, signal, ...options } = {}) {
  const headers = new Headers(options.headers || {})
  let token = null
  if (authenticated) {
    token = getAuthToken()
    if (!token) throw new ApiError('Please sign in to continue.', 401, 'MISSING_TOKEN')
    headers.set('Authorization', `Bearer ${token}`)
  }
  if (options.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')

  let response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers, signal })
  } catch (error) {
    if (error.name === 'AbortError') throw error
    throw new ApiError('Unable to connect to the server. Please try again.', 0, 'NETWORK_ERROR')
  }

  const payload = await response.json().catch(() => null)
  if (!response.ok) {
    if (authenticated && response.status === 401) {
      clearAuthSession()
      if (typeof window !== 'undefined') window.dispatchEvent(new Event('finly:session-expired'))
    }
    const message = typeof payload?.message === 'string' && payload.message.trim()
      ? payload.message
      : response.status === 401 ? 'Invalid email or password.' : `Request failed (${response.status}).`
    throw new ApiError(message, response.status, response.status === 401 ? 'UNAUTHORIZED' : 'API_ERROR')
  }
  return payload
}

export function authenticatedApiRequest(path, options = {}) {
  return apiRequest(path, { ...options, authenticated: true })
}
