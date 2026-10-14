import { useMemo, useState } from 'react'
import { EmptyState, Field, LoadingState, Message, Modal, PageHeading } from '../components/planning/PlanningUI.jsx'
import useBudgets, { currentMonthKey } from '../hooks/useBudgets.js'
import { createBudget, deleteBudget, updateBudget } from '../services/budgets.js'
import { currency } from '../utils/dashboard.js'
import '../styles/planning.css'

const blankForm = () => ({ categoryId: '', amount: '', month: currentMonthKey() })
const monthLabel = (value) => new Date(`${value}-01T12:00:00`).toLocaleDateString('en-IN', { month: 'long', year: 'numeric' })

export default function BudgetPage() {
  const month = currentMonthKey()
  const { budgets, categories, loading, error, reload } = useBudgets(month)
  const [editing, setEditing] = useState(null)
  const [modalOpen, setModalOpen] = useState(false)
  const [form, setForm] = useState(blankForm)
  const [formError, setFormError] = useState('')
  const [pageError, setPageError] = useState('')
  const [busy, setBusy] = useState(false)
  const totalLimit = useMemo(() => budgets.reduce((sum, item) => sum + Number(item.limit ?? item.amount ?? 0), 0), [budgets])
  const totalSpent = useMemo(() => budgets.reduce((sum, item) => sum + Number(item.spent ?? 0), 0), [budgets])
  const open = (budget = null) => {
    setEditing(budget)
    setModalOpen(true)
    setForm(budget ? { categoryId: String(budget.categoryId), amount: String(budget.limit ?? budget.amount), month: budget.month || month } : blankForm())
    setFormError('')
    setPageError('')
  }
  const close = () => { if (!busy) { setEditing(null); setModalOpen(false) } }
  async function submit(event) {
    event.preventDefault()
    setBusy(true); setFormError('')
    const payload = { categoryId: Number(form.categoryId), amount: Number(form.amount), month: form.month }
    try {
      if (editing) await updateBudget(editing.id, payload)
      else await createBudget(payload)
      setEditing(null); setModalOpen(false)
      await reload()
    } catch (err) { setFormError(err.message || 'Could not save this budget.') }
    finally { setBusy(false) }
  }
  async function remove(budget) {
    if (!window.confirm(`Delete the ${budget.category} budget?`)) return
    setPageError('')
    try { await deleteBudget(budget.id); await reload() }
    catch (err) { setPageError(err.message || 'Could not delete this budget.') }
  }
  const action = <button type="button" className="planning-primary" onClick={() => open()}>＋ Add Budget</button>
  return <div className="planning-page">
    <PageHeading eyebrow="Financial planning" title="Budget" description="Set monthly category limits and track your spending." action={action} />
    <Message onRetry={() => reload()}>{pageError || error}</Message>
    <div className="planning-summary-grid">
      <article className="planning-summary"><span>Total Budget</span><strong>{loading ? '—' : currency(totalLimit)}</strong><small>Across this month’s categories</small></article>
      <article className="planning-summary"><span>Total Spent</span><strong>{loading ? '—' : currency(totalSpent)}</strong><small>{monthLabel(month)}</small></article>
      <article className="planning-summary"><span>Remaining</span><strong>{loading ? '—' : currency(Math.max(0, totalLimit - totalSpent))}</strong><small>Budget limit less actual spending</small></article>
    </div>
    <section className="planning-section">
      <div className="planning-section-heading"><div><h2>Monthly Budget</h2><p>Current month · {monthLabel(month)}</p></div></div>
      {loading ? <LoadingState>Loading budgets…</LoadingState> : error ? <EmptyState title="Budgets unavailable" action={<button type="button" className="planning-secondary" onClick={() => reload()}>Try again</button>}>Your budgets could not be loaded.</EmptyState> : budgets.length === 0 ? <EmptyState title="No budgets for this month" action={action}>Create a category budget to start tracking your spending.</EmptyState> : <div className="planning-card-grid">
        {budgets.map((budget) => {
          const limit = Number(budget.limit ?? budget.amount) || 0
          const spent = Number(budget.spent) || 0
          const percent = limit > 0 ? Math.round(spent / limit * 100) : 0
          const state = percent > 100 ? 'over' : percent >= 80 ? 'warning' : ''
          return <article className={`planning-card budget-card ${state}`} key={budget.id}>
            <div className="planning-card-top"><span className="planning-card-icon">▤</span><b>{percent}% used</b></div>
            <h3>{budget.category}</h3><p className="planning-value">{currency(spent)} <span>/ {currency(limit)}</span></p>
            <div className="planning-progress"><i style={{ width: `${Math.min(percent, 100)}%` }} /></div>
            <div className="planning-card-footer"><span>{currency(Math.max(0, limit - spent))} remaining</span><div><button type="button" onClick={() => open(budget)}>Edit</button><button type="button" className="danger" onClick={() => remove(budget)}>Delete</button></div></div>
          </article>
        })}
      </div>}
    </section>
    {modalOpen && <Modal eyebrow="Monthly budget" title={editing ? 'Edit Budget' : 'Add Budget'} onClose={close} onSubmit={submit} busy={busy}>
      {formError && <p className="planning-form-error wide" role="alert">{formError}</p>}
      <Field label="Expense category"><select required value={form.categoryId} onChange={(event) => setForm({ ...form, categoryId: event.target.value })}><option value="">Select category</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select></Field>
      <Field label="Monthly budget amount"><input type="number" min="0.01" step="0.01" required value={form.amount} onChange={(event) => setForm({ ...form, amount: event.target.value })} placeholder="0" /></Field>
      <Field label="Month" wide><input type="month" required value={form.month} onChange={(event) => setForm({ ...form, month: event.target.value })} /></Field>
    </Modal>}
  </div>
}
