import { currency } from '../../utils/dashboard.js'

const categoryIcons = {
  Salary: '▣', Freelance: '✦', Food: '🍜', Transportation: '🚌', Entertainment: '▶',
  Education: '🎓', Bills: '◉', Shopping: '🛍', Healthcare: '✚', Other: '•',
}

function formatDate(value) {
  if (!value) return '—'
  const date = new Date(`${value}T00:00:00`)
  return Number.isNaN(date.getTime()) ? '—' : date.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
}

function formatPaymentMethod(value) {
  if (!value) return '—'
  const label = value.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase())
  return label === 'Upi' ? 'UPI' : label
}

export default function TransactionTable({ transactions, filteredTransactions, loading, error, onRetry, onEdit, onDelete }) {
  const rows = [...filteredTransactions].sort((a, b) => String(b.transactionDate).localeCompare(String(a.transactionDate)))
  const incomeCount = rows.filter((item) => String(item.type).toUpperCase() === 'INCOME').length
  const expenseCount = rows.length - incomeCount

  return (
    <section className="money-panel money-transactions-panel">
      <header className="money-panel-heading">
        <div><h2>Transactions</h2><p>{loading ? 'Loading your financial activity…' : `${rows.length} transaction${rows.length === 1 ? '' : 's'} shown`}</p></div>
        {rows.length > 0 && <span className="money-visible-count">{incomeCount} income · {expenseCount} expenses</span>}
      </header>
      {error ? <div className="money-message money-error" role="alert"><p>{error}</p><button type="button" onClick={onRetry}>Try again</button></div>
        : loading ? <div className="money-loading" role="status">Loading transactions…</div>
          : rows.length === 0 ? <div className="money-empty-state">
            <span aria-hidden="true">▤</span>
            <h3>{transactions.length === 0 ? 'No transactions yet' : 'No transactions match these filters'}</h3>
            <p>{transactions.length === 0 ? 'Add an income or expense to see it here.' : 'Try adjusting or clearing your filters.'}</p>
          </div>
            : <div className="money-table-scroll"><table className="money-table">
              <thead><tr><th>Date</th><th>Description</th><th>Category</th><th>Type</th><th>Amount</th><th>Payment Method</th><th><span className="sr-only">Actions</span></th></tr></thead>
              <tbody>{rows.map((item) => {
                const isIncome = String(item.type).toUpperCase() === 'INCOME'
                const categoryName = item.category?.name || 'Other'
                return <tr key={item.id}>
                  <td>{formatDate(item.transactionDate)}</td>
                  <td><span className={`money-transaction-icon ${isIncome ? 'income' : 'expense'}`} aria-hidden="true">{categoryIcons[categoryName] || '•'}</span><strong>{item.description}</strong></td>
                  <td><span className={`money-category-tag ${isIncome ? 'income-tag' : 'expense-tag'}`}>{categoryName}</span></td>
                  <td><span className={`money-type-tag ${isIncome ? 'income' : 'expense'}`}>{isIncome ? 'Income' : 'Expense'}</span></td>
                  <td className={`money-amount ${isIncome ? 'income' : 'expense'}`}>{isIncome ? '+' : '−'}{currency(item.amount)}</td>
                  <td>{formatPaymentMethod(item.paymentMethod)}</td>
                  <td><div className="money-row-actions"><button type="button" className="edit" onClick={() => onEdit(item)} aria-label={`Edit ${item.description}`}>Edit</button><button type="button" className="delete" onClick={() => onDelete(item)} aria-label={`Delete ${item.description}`}>Delete</button></div></td>
                </tr>
              })}</tbody>
            </table></div>}
    </section>
  )
}
