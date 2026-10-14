import { currency, getMonthlySeries } from '../../utils/dashboard.js'

export default function IncomeExpenseChart({ transactions, period, setPeriod, hasError }) {
  const series = getMonthlySeries(transactions, period)
  const maximum = Math.max(0, ...series.flatMap((row) => [row.income, row.expenses]))
  const axisMaximum = maximum ? Math.ceil(maximum / 10_000) * 10_000 : 0
  const ticks = [5, 4, 3, 2, 1, 0]

  return (
    <article className="dashboard-panel income-panel">
      <div className="panel-heading">
        <div><h2>Income vs Expenses</h2><p>{period === 'year' ? 'This year' : 'Last 6 months'} · income and expenses</p></div>
        <select className="dashboard-select" aria-label="Chart period" value={period} onChange={(event) => setPeriod(event.target.value)}>
          <option value="six">Last 6 months</option><option value="year">This year</option>
        </select>
      </div>
      <div className="chart-legend"><span><i className="income-dot" />Income</span><span><i className="expense-dot" />Expenses</span></div>
      <div className="chart-wrap">
        <div className="chart-axis" aria-hidden="true">{ticks.map((tick) => <span key={tick}>{axisMaximum ? currency(axisMaximum * tick / 5) : hasError ? '—' : currency(0)}</span>)}</div>
        <div className="bar-chart" role="img" aria-label={`${period === 'year' ? 'This year' : 'Last six months'} income and expenses bar chart`}>
          <div className="chart-gridlines" />
          {maximum === 0
            ? <span className="chart-empty">{hasError ? 'Transactions unavailable.' : 'No transactions in this period.'}</span>
            : series.map((row) => <div className="bar-group" key={row.label}>
              <span className="bar income-bar" style={{ height: `${row.income ? Math.max(2, row.income / maximum * 100) : 0}%` }} title={`${row.label} income: ${currency(row.income)}`} />
              <span className="bar expense-bar" style={{ height: `${row.expenses ? Math.max(2, row.expenses / maximum * 100) : 0}%` }} title={`${row.label} expenses: ${currency(row.expenses)}`} />
              <b>{row.label}</b>
            </div>)}
        </div>
      </div>
    </article>
  )
}
