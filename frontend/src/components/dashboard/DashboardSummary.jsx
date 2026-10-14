import { currency } from '../../utils/dashboard.js'

const cards = [
  { key: 'spendable', label: 'Available to Spend', helper: 'After reserved savings and subscriptions', icon: '◈', tone: 'green' },
  { key: 'income', label: 'Total Income', helper: 'Selected period', icon: '↙', tone: 'green' },
  { key: 'expenses', label: 'Total Expenses', helper: 'Selected period', icon: '↗', tone: 'orange' },
  { key: 'savings', label: 'Savings', helper: null, icon: '◎', tone: 'blue' },
]

export default function DashboardSummary({ balance, transactions, range, periodLabel, errors }) {
  const filtered = transactions.filter((item) => item.transactionDate >= range.start && item.transactionDate <= range.end)
  const totals = filtered.reduce((summary, item) => {
    const amount = Number(item.amount) || 0
    if (String(item.type).toUpperCase() === 'INCOME') summary.income += amount
    if (String(item.type).toUpperCase() === 'EXPENSE') summary.expenses += amount
    return summary
  }, { income: 0, expenses: 0 })
  const savings = totals.income - totals.expenses
  const transactionsUnavailable = Boolean(errors.transactions)
  const values = {
    spendable: balance ? currency(balance.spendableBalance) : '—',
    income: transactionsUnavailable ? '—' : currency(totals.income),
    expenses: transactionsUnavailable ? '—' : currency(totals.expenses),
    savings: transactionsUnavailable ? '—' : currency(savings),
  }
  const helpers = {
    spendable: errors.balance ? 'Balance data unavailable' : 'After reserved savings and subscriptions',
    income: periodLabel,
    expenses: periodLabel,
    savings: transactionsUnavailable ? 'Transaction data unavailable' : `${totals.income ? Math.round(savings / totals.income * 100) : 0}% of income`,
  }
  return (
    <section className="dashboard-summary" aria-label="Financial summary">
      {cards.map((card) => <article className="dashboard-summary-card" key={card.key}>
        <div className="summary-icon-row"><span className={`summary-icon ${card.tone}`} aria-hidden="true">{card.icon}</span></div>
        <p>{card.label}</p><h2>{values[card.key]}</h2><small>{helpers[card.key] ?? card.helper}</small>
      </article>)}
    </section>
  )
}
