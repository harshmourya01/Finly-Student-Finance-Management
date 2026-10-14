import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import TransactionFilters from '../components/transactions/TransactionFilters.jsx'
import TransactionFormModal from '../components/transactions/TransactionFormModal.jsx'
import TransactionTable from '../components/transactions/TransactionTable.jsx'
import useTransactions from '../hooks/useTransactions.js'
import { createTransaction, deleteTransaction, updateTransaction } from '../services/transactions.js'
import { currency, dateKey } from '../utils/dashboard.js'
import '../styles/money.css'

const emptyFilters = { search: '', category: '', method: '', date: '', amount: '' }

function monthKey(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
}

function matchesTransactionDate(transactionDate, filter) {
  if (!filter) return true
  const current = new Date()
  const currentMonth = monthKey(current)
  if (filter === 'this-month') return String(transactionDate).startsWith(currentMonth)
  if (filter === 'last-month') return String(transactionDate).startsWith(monthKey(new Date(current.getFullYear(), current.getMonth() - 1, 1)))
  return true
}

export default function MoneyPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { transactions, setTransactions, categories, balance, loading, errors, reload } = useTransactions()
  const [activeType, setActiveType] = useState('all')
  const [filters, setFilters] = useState(emptyFilters)
  const [modalOpen, setModalOpen] = useState(false)
  const [editingTransaction, setEditingTransaction] = useState(null)
  const [saving, setSaving] = useState(false)
  const [deletingId, setDeletingId] = useState(null)
  const [actionError, setActionError] = useState('')
  const autoOpened = useRef(false)

  useEffect(() => {
    if (searchParams.get('add') === '1' && !loading && !autoOpened.current) {
      autoOpened.current = true
      setEditingTransaction(null)
      setModalOpen(true)
    }
  }, [loading, searchParams])

  const visibleTransactions = useMemo(() => {
    const query = filters.search.trim().toLocaleLowerCase()
    return transactions.filter((transaction) => {
      const type = String(transaction.type || '').toUpperCase()
      if (activeType !== 'all' && type !== activeType.toUpperCase()) return false
      const categoryName = transaction.category?.name || ''
      const paymentMethod = String(transaction.paymentMethod || '')
      const searchable = [transaction.description, categoryName, paymentMethod.replaceAll('_', ' '), paymentMethod, String(transaction.amount), currency(transaction.amount)]
        .join(' ').toLocaleLowerCase()
      if (query && !searchable.includes(query)) return false
      if (filters.category && categoryName !== filters.category) return false
      if (filters.method && paymentMethod.toUpperCase() !== filters.method) return false
      if (filters.amount !== '' && Number(transaction.amount) !== Number(filters.amount)) return false
      if (!matchesTransactionDate(transaction.transactionDate, filters.date)) return false
      return true
    })
  }, [activeType, filters, transactions])

  const updateFilter = useCallback((name, value) => setFilters((current) => ({ ...current, [name]: value })), [])
  const openAdd = () => {
    setEditingTransaction(null)
    setActionError('')
    setModalOpen(true)
  }
  const openEdit = (transaction) => {
    setEditingTransaction(transaction)
    setActionError('')
    setModalOpen(true)
  }
  const closeModal = () => {
    if (saving) return
    setModalOpen(false)
    setEditingTransaction(null)
    setActionError('')
  }

  async function submitTransaction(payload) {
    setSaving(true)
    setActionError('')
    const wasEditing = Boolean(editingTransaction)
    try {
      if (wasEditing) {
        await updateTransaction(editingTransaction.id, payload)
      } else {
        await createTransaction(payload)
      }
      setModalOpen(false)
      setEditingTransaction(null)
      const shouldReturnToDashboard = !wasEditing && searchParams.get('return') === 'dashboard'
      if (shouldReturnToDashboard) navigate('/', { replace: true })
      else await reload({ showLoading: false })
    } catch (error) {
      setActionError(error.message || 'Could not save this transaction. Please try again.')
    } finally {
      setSaving(false)
    }
  }

  async function removeTransaction(transaction) {
    if (!window.confirm(`Delete “${transaction.description}”?`)) return
    setDeletingId(transaction.id)
    setActionError('')
    try {
      await deleteTransaction(transaction.id)
      setTransactions((current) => current.filter((item) => item.id !== transaction.id))
      await reload({ showLoading: false })
    } catch (error) {
      setActionError(error.message || 'Could not delete this transaction. Please try again.')
    } finally {
      setDeletingId(null)
    }
  }

  const incomeCount = transactions.filter((item) => String(item.type).toUpperCase() === 'INCOME').length
  const expenseCount = transactions.filter((item) => String(item.type).toUpperCase() === 'EXPENSE').length
  const tabChoices = [
    ['all', 'All', transactions.length], ['income', 'Income', incomeCount], ['expense', 'Expenses', expenseCount],
  ]

  return (
    <div className="money-page">
      <header className="money-page-heading">
        <div><p className="money-eyebrow">Finances</p><h1>Money</h1><p className="money-intro">Actual income and expenses. Upcoming subscription commitments remain separate until paid.</p></div>
        <button className="money-add-button" type="button" onClick={openAdd}><span aria-hidden="true">＋</span>Add Transaction</button>
      </header>

      <div className="money-tabs" role="tablist" aria-label="Transaction type">
        {tabChoices.map(([key, label, count]) => <button key={key} type="button" role="tab" aria-selected={activeType === key} className={`money-tab${activeType === key ? ' active' : ''}`} onClick={() => setActiveType(key)}>
          {label}<b>{count}</b>
        </button>)}
      </div>

      <TransactionFilters categories={categories} filters={filters} onChange={updateFilter} onClear={() => setFilters(emptyFilters)} />

      {actionError && !modalOpen && <div className="money-page-error" role="alert">{actionError}<button type="button" onClick={() => setActionError('')} aria-label="Dismiss error">×</button></div>}
      {errors.balance && !loading && <p className="money-balance-warning" role="status">Available balance could not be loaded. The server still validates expenses when you save.</p>}

      <TransactionTable transactions={transactions} filteredTransactions={visibleTransactions} loading={loading} error={errors.transactions} onRetry={() => reload()} onEdit={openEdit} onDelete={removeTransaction} />
      {deletingId !== null && <span className="sr-only" role="status">Deleting transaction…</span>}

      {modalOpen && <TransactionFormModal key={editingTransaction?.id ?? 'new'} transaction={editingTransaction} categories={categories} balance={balance} errors={{ categories: errors.categories, submit: actionError }} saving={saving} onClose={closeModal} onSubmit={submitTransaction} />}
    </div>
  )
}
