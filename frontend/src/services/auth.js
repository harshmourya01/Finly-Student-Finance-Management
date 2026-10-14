import { apiRequest, authenticatedApiRequest, clearAuthSession, getAuthToken, storeAuthSession } from './api.js'

function userFromResponse(payload) {
  if (!payload?.user || typeof payload.user.fullName !== 'string' || !payload.user.fullName.trim()) {
    throw new Error('The server returned an incomplete account profile.')
  }
  return payload.user
}

export async function login(email, password) {
  clearAuthSession()
  const payload = await apiRequest('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email: email.trim(), password }),
  })
  if (typeof payload?.token !== 'string' || !payload.token) throw new Error('The server did not return an authentication token.')
  const user = userFromResponse(payload)
  storeAuthSession(payload.token, user)
  return user
}

export async function registerAccount(fullName, email, password) {
  const payload = await apiRequest('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify({ fullName: fullName.trim(), email: email.trim(), password }),
  })
  if (typeof payload?.token !== 'string' || !payload.token) throw new Error('The server did not return an authentication token.')
  userFromResponse(payload)
  return payload.user
}

export async function getCurrentUser() {
  const user = await authenticatedApiRequest('/api/auth/me')
  if (!user || typeof user.fullName !== 'string' || !user.fullName.trim()) {
    clearAuthSession()
    throw new Error('The server returned an incomplete account profile.')
  }
  const token = getAuthToken()
  if (token) storeAuthSession(token, user)
  return user
}

export function logout() {
  clearAuthSession()
}

export { clearAuthSession, getAuthToken }
