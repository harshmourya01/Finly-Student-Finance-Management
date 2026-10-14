import { useState } from 'react'
import { EmptyState, Field, LoadingState, Message, Modal, PageHeading } from '../components/planning/PlanningUI.jsx'
import useSavingsGoals from '../hooks/useSavingsGoals.js'
import useBalanceSummary from '../hooks/useBalanceSummary.js'
import { addSavingsGoalMoney, createSavingsGoal, deleteSavingsGoal, updateSavingsGoal } from '../services/savingsGoals.js'
import { currency } from '../utils/dashboard.js'
import '../styles/planning.css'

const blankGoal = () => ({ name: '', targetAmount: '', targetDate: new Date().toISOString().slice(0, 7) })
const savedOf = (goal) => Number(goal.saved ?? goal.currentAmount ?? 0)
const targetOf = (goal) => Number(goal.target ?? goal.targetAmount ?? 0)
const dateOf = (goal) => goal.date || (goal.targetDate ? String(goal.targetDate).slice(0, 7) : '')

export default function SavingsGoalsPage() {
  const { goals, loading, error, reload } = useSavingsGoals()
  const { balance, error: balanceError, reload: reloadBalance } = useBalanceSummary()
  const [mode, setMode] = useState('')
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(blankGoal)
  const [contribution, setContribution] = useState('')
  const [formError, setFormError] = useState('')
  const [pageError, setPageError] = useState('')
  const [busy, setBusy] = useState(false)
  const openGoal = (goal = null) => {
    setMode('goal'); setEditing(goal)
    setForm(goal ? { name: goal.name, targetAmount: String(targetOf(goal)), targetDate: dateOf(goal) } : blankGoal())
    setFormError(''); setPageError('')
  }
  const openContribution = (goal) => { setEditing(goal); setContribution(''); setFormError(''); setPageError(''); setMode('contribution') }
  const close = () => { if (!busy) { setMode(''); setEditing(null) } }
  async function submitGoal(event) {
    event.preventDefault(); setBusy(true); setFormError('')
    const payload = { name: form.name.trim(), targetAmount: Number(form.targetAmount), targetDate: form.targetDate }
    try {
      if (editing) await updateSavingsGoal(editing.id, payload)
      else await createSavingsGoal(payload)
      setMode(''); await reload()
    } catch (err) { setFormError(err.message || 'Could not save this savings goal.') }
    finally { setBusy(false) }
  }
  async function submitContribution(event) {
    event.preventDefault(); setBusy(true); setFormError('')
    try { await addSavingsGoalMoney(editing.id, Number(contribution)); setMode(''); await Promise.all([reload(), reloadBalance()]) }
    catch (err) { setFormError(err.message || 'Could not add money to this goal.') }
    finally { setBusy(false) }
  }
  async function remove(goal) {
    if (!window.confirm(`Delete “${goal.name}”?`)) return
    setPageError('')
    try { await deleteSavingsGoal(goal.id); await reload() }
    catch (err) { setPageError(err.message || 'Could not delete this goal.') }
  }
  const addButton = <button type="button" className="planning-primary" onClick={() => openGoal()}>＋ Add Goal</button>
  return <div className="planning-page">
    <PageHeading eyebrow="Savings" title="Savings Goals" description="Track your targets and the amount you have contributed." action={addButton} />
    <div className="planning-summary-grid planning-spendable-grid"><article className="planning-summary spendable-summary"><span>Available to Spend</span><strong>{balance ? currency(balance.spendableBalance) : '—'}</strong><small>{balanceError || 'Calculated by your account balance summary'}</small></article></div>
    <Message onRetry={() => reload()}>{pageError || error}</Message>
    {loading ? <LoadingState>Loading savings goals…</LoadingState> : error ? <EmptyState title="Savings goals unavailable" action={<button type="button" className="planning-secondary" onClick={() => reload()}>Try again</button>}>Your goals could not be loaded.</EmptyState> : goals.length === 0 ? <EmptyState title="No savings goals yet" action={addButton}>Create a goal to track the money you are saving toward it.</EmptyState> : <div className="planning-card-grid goals-grid">
      {goals.map((goal) => {
        const saved = savedOf(goal); const target = targetOf(goal)
        const percent = target > 0 ? Math.round(saved / target * 100) : 0
        return <article className="planning-card savings-goal-card" key={goal.id}>
          <div className="savings-goal-header">
            <div className="savings-goal-identity"><span className="savings-goal-icon" aria-hidden="true">✦</span><div className="savings-goal-name"><h3>{goal.name}</h3><p>Target date: {dateOf(goal) ? new Date(`${dateOf(goal)}-01T12:00:00`).toLocaleDateString('en-IN', { month: 'long', year: 'numeric' }) : '—'}</p></div></div>
            <div className="planning-card-actions"><button type="button" onClick={() => openGoal(goal)}>Edit</button><button type="button" className="danger" onClick={() => remove(goal)}>Delete</button></div>
          </div>
          <div className="savings-goal-target"><span>Target amount</span><strong>{currency(target)}</strong></div>
          <div className="savings-goal-metrics">
            <div className="savings-goal-metric"><span>Saved</span><strong>{currency(saved)}</strong></div>
            <div className="savings-goal-metric"><span>Remaining</span><strong>{currency(Math.max(0, target - saved))}</strong></div>
            <div className="savings-goal-metric"><span>Progress</span><strong>{percent}%</strong></div>
          </div>
          <div className="savings-goal-progress" role="progressbar" aria-label={`${goal.name} progress`} aria-valuemin="0" aria-valuemax="100" aria-valuenow={Math.min(Math.max(percent, 0), 100)}><i style={{ width: `${Math.min(Math.max(percent, 0), 100)}%` }} /></div>
          <div className="savings-goal-action"><button type="button" className="planning-primary contribution-button" onClick={() => openContribution(goal)}>Add Money</button></div>
        </article>
      })}
    </div>}
    {mode === 'goal' && <Modal eyebrow="Savings goal" title={editing ? 'Edit Savings Goal' : 'Add Savings Goal'} onClose={close} onSubmit={submitGoal} busy={busy}>
      {formError && <p className="planning-form-error wide" role="alert">{formError}</p>}
      <Field label="Goal name" wide><input required maxLength="120" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} placeholder="e.g. New Laptop" /></Field>
      <Field label="Target amount"><input type="number" min="0.01" step="0.01" required value={form.targetAmount} onChange={(event) => setForm({ ...form, targetAmount: event.target.value })} placeholder="0" /></Field>
      <Field label="Target date" wide><input type="month" required value={form.targetDate} onChange={(event) => setForm({ ...form, targetDate: event.target.value })} /></Field>
    </Modal>}
    {mode === 'contribution' && <Modal eyebrow="Add to savings" title={`Add Money · ${editing?.name || ''}`} onClose={close} onSubmit={submitContribution} busy={busy} submitLabel="Add Money">
      {formError && <p className="planning-form-error wide" role="alert">{formError}</p>}
      <Field label="Amount"><input type="number" min="0.01" step="0.01" required value={contribution} onChange={(event) => setContribution(event.target.value)} placeholder="0" /></Field>
      <p className="planning-hint wide">Remaining target: {currency(Math.max(0, targetOf(editing) - savedOf(editing)))}. The server will validate this contribution.</p>
    </Modal>}
  </div>
}
