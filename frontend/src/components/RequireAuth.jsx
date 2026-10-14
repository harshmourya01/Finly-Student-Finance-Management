import { useCallback, useEffect, useState } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { ApiError } from '../services/api.js'
import { clearAuthSession, getAuthToken, getCurrentUser } from '../services/auth.js'

export default function RequireAuth() {
  const location = useLocation()
  const [status, setStatus] = useState('checking')
  const [error, setError] = useState('')
  const [attempt, setAttempt] = useState(0)

  const verify = useCallback(async () => {
    if (!getAuthToken()) {
      setStatus('signed-out')
      return
    }
    setStatus('checking')
    setError('')
    try {
      await getCurrentUser()
      setStatus('authenticated')
    } catch (reason) {
      if (reason instanceof ApiError && reason.status === 401) {
        clearAuthSession()
        setStatus('signed-out')
      } else {
        setError(reason.message || 'Could not verify your session. Please try again.')
        setStatus('error')
      }
    }
  }, [])

  useEffect(() => { verify() }, [verify, attempt])

  useEffect(() => {
    const onSessionExpired = () => setStatus('signed-out')
    window.addEventListener('finly:session-expired', onSessionExpired)
    return () => window.removeEventListener('finly:session-expired', onSessionExpired)
  }, [])

  if (status === 'signed-out') return <Navigate to="/login" replace state={{ from: location }} />
  if (status === 'checking') return <main className="auth-loading" role="status">Verifying your session…</main>
  if (status === 'error') return (
    <main className="auth-loading">
      <section className="auth-error-card" role="alert">
        <h1>Could not verify your session</h1>
        <p>{error}</p>
        <button type="button" className="auth-submit" onClick={() => setAttempt((value) => value + 1)}>Try again</button>
        <button type="button" className="auth-text-button" onClick={() => { clearAuthSession(); setStatus('signed-out') }}>Return to Login</button>
      </section>
    </main>
  )
  return <Outlet />
}
