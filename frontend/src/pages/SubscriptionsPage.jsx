import { useMemo, useState } from 'react'
import { EmptyState, Field, LoadingState, Message, Modal, PageHeading } from '../components/planning/PlanningUI.jsx'
import useSubscriptions from '../hooks/useSubscriptions.js'
import useBalanceSummary from '../hooks/useBalanceSummary.js'
import { createSubscription, deleteSubscription, markSubscriptionPaid, updateSubscription } from '../services/subscriptions.js'
import { Link } from 'react-router-dom'
import { currency } from '../utils/dashboard.js'
import '../styles/planning.css'

const blankSubscription = () => ({ name: '', category: '', amount: '', frequency: 'MONTHLY', renewal: new Date().toISOString().slice(0, 10), status: 'ACTIVE' })
const dateLabel = (value) => value ? new Date(`${value}T12:00:00`).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : '—'

function subscriptionMonthlyCents(item) {
  const amountCents = Math.round(Number(item.amount) * 100)
  if (item.frequency === 'MONTHLY') return amountCents
  // Backend: yearly amount / 12, rounded HALF_UP to two decimal places.
  return Math.floor(amountCents / 12 + 0.5)
}

function formatCents(cents) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', minimumFractionDigits: cents % 100 ? 2 : 0, maximumFractionDigits: 2 }).format(cents / 100)
}

export default function SubscriptionsPage() {
  const { subscriptions, loading, error, reload } = useSubscriptions()
  const { balance, error: balanceError, reload: reloadBalance } = useBalanceSummary()
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(blankSubscription)
  const [formError, setFormError] = useState('')
  const [pageError, setPageError] = useState('')
  const [busy, setBusy] = useState(false)
  const [payingId, setPayingId] = useState(null)
  const [paidThisView, setPaidThisView] = useState(() => new Set())
  const [todayKey] = useState(() => {
    const date = new Date(); date.setHours(0, 0, 0, 0)
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
  })
  const active = useMemo(() => subscriptions.filter((item) => item.status === 'ACTIVE'), [subscriptions])
  const monthlyCents = useMemo(() => active.reduce((sum, item) => sum + subscriptionMonthlyCents(item), 0), [active])
  const upcoming = useMemo(() => active.filter((item) => item.renewal && item.renewal >= todayKey).length, [active, todayKey])
  const open = (item = null) => {
    setEditing(item)
    setForm(item ? { name: item.name, category: item.category, amount: String(item.amount), frequency: item.frequency, renewal: item.renewal, status: item.status } : blankSubscription())
    setFormError(''); setPageError(''); setModalOpen(true)
  }
  const close = () => { if (!busy) { setModalOpen(false); setEditing(null) } }
  async function submit(event) {
    event.preventDefault(); setBusy(true); setFormError('')
    const payload = { ...form, name: form.name.trim(), category: form.category.trim(), amount: Number(form.amount) }
    try {
      if (editing) await updateSubscription(editing.id, payload)
      else await createSubscription(payload)
      setModalOpen(false); setEditing(null); await Promise.all([reload(), reloadBalance()])
    } catch (err) { setFormError(err.message || 'Could not save this subscription.') }
    finally { setBusy(false) }
  }
  async function remove(item) {
    if (!window.confirm(`Delete “${item.name}”?`)) return
    setPageError('')
    try { await deleteSubscription(item.id); await Promise.all([reload(), reloadBalance()]) }
    catch (err) { setPageError(err.message || 'Could not delete this subscription.') }
  }
  async function markPaid(item) {
    setPayingId(item.id); setPageError('')
    try {
      await markSubscriptionPaid(item.id)
      setPaidThisView((current) => new Set(current).add(item.id))
      await Promise.all([reload(), reloadBalance()])
    } catch (err) { setPageError(err.message || 'Could not record this subscription payment.') }
    finally { setPayingId(null) }
  }
  const addButton = <button type="button" className="planning-primary" onClick={() => open()}>＋ Add Subscription</button>
  return <div className="planning-page subscriptions-page">
    <PageHeading eyebrow="Recurring payments" title="Subscriptions" description="Keep track of your recurring payments and renewals." action={addButton} />
    <Message onRetry={() => reload()}>{pageError || error}</Message>
    <div className="planning-summary-grid subscription-summary-grid subscriptions-summary-grid">
      <article className="planning-summary spendable-summary subscription-summary-card"><span>Available to Spend</span><strong>{balance ? currency(balance.spendableBalance) : '—'}</strong><small>{balanceError || 'From the backend balance summary'}</small></article>
      <article className="planning-summary subscription-summary-card"><span>Monthly Subscription Cost</span><strong>{loading ? '—' : formatCents(monthlyCents)}</strong><small>Active commitments, yearly amounts divided by 12</small></article>
      <article className="planning-summary subscription-summary-card"><span>Active Subscriptions</span><strong>{loading ? '—' : active.length}</strong><small>Currently marked active</small></article>
      <article className="planning-summary subscription-summary-card"><span>Upcoming Renewals</span><strong>{loading ? '—' : upcoming}</strong><small>Active subscriptions due from today onward</small></article>
    </div>
    <section className="planning-section subscriptions-section">
      <div className="planning-section-heading subscriptions-section-heading"><div><h2>Your Subscriptions</h2><p>Manage billing cycle, status and next renewal.</p></div></div>
      {loading ? <LoadingState>Loading subscriptions…</LoadingState> : error ? <EmptyState title="Subscriptions unavailable" action={<button type="button" className="planning-secondary" onClick={() => reload()}>Try again</button>}>Your subscriptions could not be loaded.</EmptyState> : subscriptions.length === 0 ? <EmptyState title="No subscriptions yet" action={addButton}>Add your recurring payments to track monthly commitments.</EmptyState> : <div className="planning-table-wrap subscriptions-table-wrap" role="region" aria-label="Your subscriptions" tabIndex="0"><table className="planning-table subscriptions-table"><thead><tr><th>Status</th><th>Name</th><th>Category</th><th>Frequency</th><th>Amount</th><th>Monthly commitment</th><th>Next renewal</th><th>Actions</th></tr></thead><tbody>
        {subscriptions.map((item) => <tr key={item.id}><td><span className={`planning-status ${item.status === 'ACTIVE' ? 'active' : 'stopped'}`}>{item.status === 'ACTIVE' ? 'Active' : 'Stopped'}</span></td><td className="subscription-name">{item.name}</td><td>{item.category}</td><td>{item.frequency === 'YEARLY' ? 'Yearly' : 'Monthly'}</td><td className={item.status === 'ACTIVE' ? 'subscription-amount' : ''}>{currency(item.amount)}</td><td>{item.status === 'ACTIVE' ? formatCents(subscriptionMonthlyCents(item)) : '—'}</td><td>{dateLabel(item.renewal)}</td><td><div className="planning-card-actions planning-subscription-actions">{item.status === 'ACTIVE' && <button type="button" className="paid-action" disabled={payingId === item.id || paidThisView.has(item.id)} onClick={() => markPaid(item)}>{paidThisView.has(item.id) ? 'Paid this cycle' : payingId === item.id ? 'Saving…' : 'Mark as Paid'}</button>}<button type="button" className="subscription-secondary-action" onClick={() => open(item)}>Edit</button><button type="button" className="danger subscription-secondary-action" onClick={() => remove(item)}>Delete</button></div></td></tr>)}
      </tbody></table></div>}
    </section>
    {paidThisView.size > 0 && <p className="planning-paid-note" role="status">Payment recorded as an actual expense. <Link to="/money">View transactions</Link></p>}
    {modalOpen && <Modal eyebrow="Subscription" title={editing ? 'Edit Subscription' : 'Add Subscription'} onClose={close} onSubmit={submit} busy={busy}>
      {formError && <p className="planning-form-error wide" role="alert">{formError}</p>}
      <Field label="Subscription name"><input required value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} placeholder="e.g. Streaming" /></Field>
      <Field label="Category"><input required value={form.category} onChange={(event) => setForm({ ...form, category: event.target.value })} placeholder="e.g. Entertainment" /></Field>
      <Field label="Amount"><input type="number" min="0.01" step="0.01" required value={form.amount} onChange={(event) => setForm({ ...form, amount: event.target.value })} placeholder="0" /></Field>
      <Field label="Billing cycle"><select value={form.frequency} onChange={(event) => setForm({ ...form, frequency: event.target.value })}><option value="MONTHLY">Monthly</option><option value="YEARLY">Yearly</option></select></Field>
      <Field label="Next renewal date"><input type="date" required value={form.renewal} onChange={(event) => setForm({ ...form, renewal: event.target.value })} /></Field>
      <Field label="Status"><select value={form.status} onChange={(event) => setForm({ ...form, status: event.target.value })}><option value="ACTIVE">Active</option><option value="STOPPED">Stopped</option></select></Field>
    </Modal>}
  </div>
}
