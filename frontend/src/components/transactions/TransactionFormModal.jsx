import { useEffect, useState } from 'react'
import { currency, dateKey } from '../../utils/dashboard.js'

const paymentMethods = [
  ['UPI', 'UPI'], ['CASH', 'Cash'], ['CREDIT_CARD', 'Credit Card'],
  ['DEBIT_CARD', 'Debit Card'], ['BANK_TRANSFER', 'Bank Transfer'], ['OTHER', 'Other'],
]

function initialForm(transaction) {
  if (!transaction) return { type: 'EXPENSE', amount: '', description: '', categoryId: '', date: dateKey(new Date()), paymentMethod: 'UPI', notes: '' }
  return {
    type: String(transaction.type).toUpperCase(),
    amount: String(transaction.amount ?? ''),
    description: transaction.description ?? '',
    categoryId: transaction.category?.id ? String(transaction.category.id) : '',
    date: transaction.transactionDate ?? dateKey(new Date()),
    paymentMethod: transaction.paymentMethod ?? 'UPI',
    notes: transaction.notes ?? '',
  }
}

export default function TransactionFormModal({ transaction, categories, balance, errors, saving, onClose, onSubmit }) {
  const [form, setForm] = useState(() => initialForm(transaction))
  const [validationError, setValidationError] = useState('')
  const filteredCategories = categories.filter((category) => String(category.type).toUpperCase() === form.type)
  const isEditing = Boolean(transaction)
  const setField = (field, value) => {
    setForm((current) => ({ ...current, [field]: value }))
    setValidationError('')
  }

  useEffect(() => {
    const closeOnEscape = (event) => { if (event.key === 'Escape' && !saving) onClose() }
    document.addEventListener('keydown', closeOnEscape)
    return () => document.removeEventListener('keydown', closeOnEscape)
  }, [onClose, saving])

  async function submit(event) {
    event.preventDefault()
    if (!form.categoryId) {
      setValidationError('Please select a category.')
      return
    }
    const amount = Number(form.amount)
    if (!Number.isFinite(amount) || amount <= 0) {
      setValidationError('Enter an amount greater than zero.')
      return
    }
    const description = form.description.trim()
    if (!description) {
      setValidationError('Description is required.')
      return
    }
    await onSubmit({
      amount,
      transactionDate: form.date,
      description,
      type: form.type,
      paymentMethod: form.paymentMethod,
      categoryId: Number(form.categoryId),
      notes: form.notes.trim(),
    })
  }

  return (
    <div className="money-modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget && !saving) onClose() }}>
      <section className="money-transaction-modal" role="dialog" aria-modal="true" aria-labelledby="transaction-modal-title">
        <header className="money-modal-heading">
          <div><p>{isEditing ? 'UPDATE ENTRY' : 'NEW ENTRY'}</p><h2 id="transaction-modal-title">{isEditing ? 'Edit Transaction' : 'Add Transaction'}</h2></div>
          <button type="button" className="money-modal-close" onClick={onClose} disabled={saving} aria-label="Close">×</button>
        </header>
        <form className="money-form" onSubmit={submit}>
          {form.type === 'EXPENSE' && <p className={`money-spendable-note${errors.balance ? ' unavailable' : ''}`} role="status" aria-live="polite">
            {balance ? <>Available to spend: <strong>{currency(balance.spendableBalance)}</strong></> : 'Available balance is unavailable. The server still validates expenses when you save.'}
          </p>}
          <div className="money-form-grid">
            <label>Transaction Type<select value={form.type} onChange={(event) => setForm((current) => ({ ...current, type: event.target.value, categoryId: '' }))} required>
              <option value="EXPENSE">Expense</option><option value="INCOME">Income</option>
            </select></label>
            <label>Amount<input type="number" min="0.01" step="0.01" value={form.amount} onChange={(event) => setField('amount', event.target.value)} placeholder="0" required /></label>
            <label>Description<input type="text" value={form.description} onChange={(event) => setField('description', event.target.value)} placeholder="e.g. Grocery" required /></label>
            <label>Category<select value={form.categoryId} onChange={(event) => setField('categoryId', event.target.value)} required>
              <option value="">Select category</option>{filteredCategories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
            </select></label>
            <label>Date<input type="date" value={form.date} onChange={(event) => setField('date', event.target.value)} required /></label>
            <label>Payment Method<select value={form.paymentMethod} onChange={(event) => setField('paymentMethod', event.target.value)} required>
              {paymentMethods.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
            </select></label>
            <label className="money-notes-field">Notes <span>(optional)</span><textarea value={form.notes} onChange={(event) => setField('notes', event.target.value)} placeholder="Add a note (optional)" /></label>
          </div>
          {(validationError || errors.categories || errors.submit) && <p className="money-form-error" role="alert">{validationError || errors.categories || errors.submit}</p>}
          <footer className="money-modal-actions">
            <button type="button" className="money-cancel-button" onClick={onClose} disabled={saving}>Cancel</button>
            <button type="submit" className="money-submit-button" disabled={saving || categories.length === 0}>
              {saving ? 'Saving…' : isEditing ? 'Save Changes' : 'Add Transaction'}
            </button>
          </footer>
        </form>
      </section>
    </div>
  )
}
