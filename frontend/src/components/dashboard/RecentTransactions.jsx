import { Link } from 'react-router-dom'
import { currency } from '../../utils/dashboard.js'

function displayDate(value) {
  if (!value) return '—'
  const date = new Date(`${value}T00:00:00`)
  return Number.isNaN(date.getTime()) ? '—' : date.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })
}

export default function RecentTransactions({ transactions, hasError }) {
  const latest = [...transactions].sort((a, b) => String(b.transactionDate).localeCompare(String(a.transactionDate))).slice(0, 5)
  return (
    <section className="dashboard-panel transactions-panel">
      <div className="panel-heading transaction-heading"><div><h2>Recent Transactions</h2><p>Your latest financial activity</p></div>
        <Link className="add-transaction" to="/money?add=1&return=dashboard"><span aria-hidden="true">＋</span> Add Transaction</Link>
      </div>
      {latest.length ? <div className="transaction-table-wrap"><table className="transaction-table">
        <thead><tr><th>Date</th><th>Description</th><th>Category</th><th>Type</th><th>Amount</th><th>Payment Method</th></tr></thead>
        <tbody>{latest.map((transaction) => {
          const income = String(transaction.type).toUpperCase() === 'INCOME'
          return <tr key={transaction.id}>
            <td>{displayDate(transaction.transactionDate)}</td><td><strong>{transaction.description || '—'}</strong></td>
            <td>{transaction.category?.name || 'Other'}</td><td><span className={`transaction-type ${income ? 'income-type' : 'expense-type'}`}>{income ? 'Income' : 'Expense'}</span></td>
            <td className={`transaction-amount ${income ? 'income-amount' : 'expense-amount'}`}>{income ? '+' : '−'}{currency(transaction.amount)}</td>
            <td>{String(transaction.paymentMethod || '—').replaceAll('_', ' ')}</td>
          </tr>
        })}</tbody>
      </table></div> : <p className="dashboard-empty transaction-empty">{hasError ? 'Transactions could not be loaded.' : 'No transactions yet.'}</p>}
    </section>
  )
}
