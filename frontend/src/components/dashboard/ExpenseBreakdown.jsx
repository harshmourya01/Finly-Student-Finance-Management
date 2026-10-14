import { currency, getExpenseCategories } from '../../utils/dashboard.js'

const palette = Array.from({ length: 7 }, (_, index) => `var(--dashboard-chart-${index + 1})`)

export default function ExpenseBreakdown({ transactions, range, hasError }) {
  const entries = getExpenseCategories(transactions, range)
  const total = entries.reduce((sum, entry) => sum + entry.amount, 0)
  let offset = 0
  const background = total
    ? `conic-gradient(${entries.map((entry, index) => {
      const start = offset
      offset += entry.amount / total * 100
      return `${palette[index % palette.length]} ${start}% ${offset}%`
    }).join(', ')})`
    : 'conic-gradient(var(--chart-empty) 0 100%)'

  return (
    <article className="dashboard-panel breakdown-panel">
      <div className="panel-heading"><div><h2>Expense Breakdown</h2><p>Where your money goes</p></div></div>
      <div className="donut-wrap"><div className="donut-chart" style={{ background }} role="img" aria-label={`Expense breakdown, ${hasError ? 'unavailable' : `${currency(total)} total`}`}>
        <div><strong>{hasError ? '—' : currency(total)}</strong><span>{hasError ? 'Unavailable' : 'Total spent'}</span></div>
      </div></div>
      <div className="breakdown-list">
        {!entries.length
          ? <p className="dashboard-empty">{hasError ? 'Transactions unavailable.' : 'No expenses for this period.'}</p>
          : entries.map((entry, index) => <div className="breakdown-row" key={entry.name}>
            <i style={{ backgroundColor: palette[index % palette.length] }} /><span>{entry.name}</span>
            <b>{currency(entry.amount)}</b><em>{Math.round(entry.amount / total * 100)}%</em>
          </div>)}
      </div>
    </article>
  )
}
