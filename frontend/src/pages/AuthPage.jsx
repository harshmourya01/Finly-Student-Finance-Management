import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { login, logout, registerAccount } from '../services/auth.js'
import '../styles/auth.css'

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export default function AuthPage({ mode }) {
  const isRegister = mode === 'register'
  const navigate = useNavigate()
  const location = useLocation()
  const [values, setValues] = useState({ fullName: '', email: '', password: '', confirmPassword: '' })
  const [errors, setErrors] = useState({})
  const [message, setMessage] = useState(location.state?.notice ?? '')
  const [messageKind, setMessageKind] = useState(location.state?.notice ? 'success' : '')
  const [submitting, setSubmitting] = useState(false)

  const update = (field) => (event) => {
    setValues((current) => ({ ...current, [field]: event.target.value }))
    setErrors((current) => ({ ...current, [field]: '' }))
    setMessage('')
  }

  function validate() {
    const next = {}
    if (isRegister && !values.fullName.trim()) next.fullName = 'Full name is required.'
    if (!values.email.trim()) next.email = 'Email is required.'
    else if (!emailPattern.test(values.email.trim())) next.email = 'Enter a valid email address.'
    if (!values.password) next.password = 'Password is required.'
    else if (isRegister && values.password.length < 6) next.password = 'Password must be at least 6 characters.'
    if (isRegister && !values.confirmPassword) next.confirmPassword = 'Please confirm your password.'
    else if (isRegister && values.password !== values.confirmPassword) next.confirmPassword = 'Passwords do not match.'
    return next
  }

  async function submit(event) {
    event.preventDefault()
    const validationErrors = validate()
    setErrors(validationErrors)
    setMessage('')
    if (Object.keys(validationErrors).length) return

    setSubmitting(true)
    try {
      if (isRegister) {
        await registerAccount(values.fullName, values.email, values.password)
        logout()
        navigate('/login', {
          replace: true,
          state: { notice: 'Account created successfully. Sign in to continue.' },
        })
      } else {
        await login(values.email, values.password)
        const destination = location.state?.from?.pathname || '/'
        navigate(destination, { replace: true })
      }
    } catch (error) {
      const detail = error.message || (isRegister ? 'Registration failed. Please try again.' : 'Invalid email or password.')
      if (isRegister && /email.*(registered|exists|already)|already.*email/i.test(detail)) {
        setErrors((current) => ({ ...current, email: detail }))
        setMessage('')
      } else {
        setMessage(detail)
        setMessageKind('error')
      }
    } finally {
      setSubmitting(false)
    }
  }

  function showRecoveryMessage() {
    setMessage('Password recovery will be available in a future update.')
    setMessageKind('info')
  }

  return (
    <main className="auth-page">
      <Link className="auth-brand" to="/" aria-label="Finly home"><span aria-hidden="true">₹</span>Finly</Link>
      <section className="auth-card" aria-labelledby="auth-title">
        <div className="auth-intro">
          <p className="auth-eyebrow">{isRegister ? 'GET STARTED' : 'WELCOME BACK'}</p>
          <h1 id="auth-title">{isRegister ? 'Create your account' : 'Welcome back'}</h1>
          <p>{isRegister ? 'Start managing your finances today.' : 'Manage your finances smarter.'}</p>
        </div>
        <form className="auth-form" onSubmit={submit} noValidate>
          {isRegister && <AuthField label="Full Name" name="fullName" value={values.fullName} onChange={update('fullName')} error={errors.fullName} type="text" autoComplete="name" placeholder="Your full name" />}
          <AuthField label="Email" name="email" value={values.email} onChange={update('email')} error={errors.email} type="email" autoComplete="email" placeholder="you@example.com" />
          <AuthField label="Password" name="password" value={values.password} onChange={update('password')} error={errors.password} type="password" autoComplete={isRegister ? 'new-password' : 'current-password'} placeholder={isRegister ? 'Create a password' : 'Enter your password'} />
          {isRegister
            ? <AuthField label="Confirm Password" name="confirmPassword" value={values.confirmPassword} onChange={update('confirmPassword')} error={errors.confirmPassword} type="password" autoComplete="new-password" placeholder="Confirm your password" />
            : <div className="auth-options"><span aria-hidden="true" /> <button type="button" className="auth-inline-link" onClick={showRecoveryMessage}>Forgot password?</button></div>}
          {message && <p className={`auth-message ${messageKind}`} role={messageKind === 'error' ? 'alert' : 'status'}>{message}</p>}
          <button className="auth-submit" type="submit" disabled={submitting}>
            {submitting ? (isRegister ? 'Creating Account…' : 'Logging in…') : (isRegister ? 'Create Account' : 'Login')}
          </button>
        </form>
        <p className="auth-switch">
          {isRegister ? 'Already have an account? ' : "Don't have an account? "}
          <Link to={isRegister ? '/login' : '/register'}>{isRegister ? 'Login' : 'Sign up'}</Link>
        </p>
      </section>
      <p className="auth-note">Student Financial Planning System</p>
    </main>
  )
}

function AuthField({ label, name, value, onChange, error, type, autoComplete, placeholder }) {
  const errorId = `${name}-error`
  return (
    <label className="auth-field" htmlFor={name}>
      <span>{label}</span>
      <input id={name} name={name} type={type} value={value} onChange={onChange} autoComplete={autoComplete} placeholder={placeholder} aria-invalid={Boolean(error)} aria-describedby={error ? errorId : undefined} />
      {error && <small className="auth-field-error" id={errorId}>{error}</small>}
    </label>
  )
}
