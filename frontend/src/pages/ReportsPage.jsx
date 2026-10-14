import { useEffect, useMemo, useState } from 'react'
import { loadReportData, downloadCsv } from '../services/reports.js'

const types = ['Monthly Financial Summary', 'Income Report', 'Expense Report', 'Budget Report', 'Savings Report', 'Subscription Report']
const currency = (value) => `₹${Math.round(Number(value) || 0).toLocaleString('en-IN')}`
const dateKey = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
const monthStart = (date, offset = 0) => new Date(date.getFullYear(), date.getMonth() + offset, 1)
function getRange(period, from, to) {
  const now = new Date(); const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  if (period === 'custom') return { start: from, end: to }
  if (period === 'last') return { start: dateKey(monthStart(today, -1)), end: dateKey(new Date(today.getFullYear(), today.getMonth(), 0)) }
  if (period === 'six') return { start: dateKey(monthStart(today, -5)), end: dateKey(today) }
  if (period === 'year') return { start: `${today.getFullYear()}-01-01`, end: dateKey(today) }
  return { start: dateKey(monthStart(today)), end: dateKey(today) }
}
const typeOf = (item, type) => String(item.type).toUpperCase() === type
const sumType = (items, type) => items.filter((item) => typeOf(item, type)).reduce((sum, item) => sum + Number(item.amount || 0), 0)
const byCategory = (items) => {
  const values = new Map()
  items.forEach((item) => { const name = item.category?.name || 'Other'; values.set(name, (values.get(name) || 0) + Number(item.amount || 0)) })
  return [...values.entries()].sort((a, b) => b[1] - a[1])
}
const showDate = (value) => value ? new Date(`${value}T00:00:00`).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : '—'

export default function ReportsPage() {
  const [data, setData] = useState({ transactions: [], budgets: [], goals: [], subscriptions: [], partialError: '' })
  const [loading, setLoading] = useState(true); const [error, setError] = useState(''); const [attempt, setAttempt] = useState(0)
  const [type, setType] = useState(types[0]); const [period, setPeriod] = useState('month'); const [from, setFrom] = useState(''); const [to, setTo] = useState('')
  useEffect(() => {
    const controller = new AbortController(); setLoading(true); setError('')
    loadReportData(controller.signal).then(setData).catch((reason) => { if (!controller.signal.aborted) setError(reason.message || 'Could not load report data.') }).finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [attempt])
  const report = useMemo(() => {
    const range = getRange(period, from, to); const valid = Boolean(range.start && range.end && range.start <= range.end)
    const transactions = valid ? data.transactions.filter((item) => item.transactionDate >= range.start && item.transactionDate <= range.end) : []
    const income = sumType(transactions, 'INCOME'); const expense = sumType(transactions, 'EXPENSE'); const net = income - expense
    const expensesByCategory = byCategory(transactions.filter((item) => typeOf(item, 'EXPENSE')))
    let headers = []; let rows = []
    if (type === 'Monthly Financial Summary' || type === 'Expense Report') {
      headers = type === 'Monthly Financial Summary' ? ['Category', 'Amount', 'Share of expenses'] : ['Category', 'Expenses', 'Share']
      rows = expensesByCategory.map(([name, value]) => [name, currency(value), expense ? `${Math.round(value / expense * 100)}%` : '0%'])
    } else if (type === 'Income Report') {
      headers = ['Category', 'Income']; rows = byCategory(transactions.filter((item) => typeOf(item, 'INCOME'))).map(([name, value]) => [name, currency(value)])
    } else if (type === 'Budget Report') {
      headers = ['Month', 'Category', 'Budget', 'Spent', 'Remaining']
      rows = data.budgets.filter((budget) => budget.month && budget.month >= range.start.slice(0, 7) && budget.month <= range.end.slice(0, 7)).map((budget) => {
        const limit = Number(budget.limit ?? budget.amount ?? 0); const spent = Number(budget.spent || 0)
        return [budget.month, budget.category || 'Other', currency(limit), currency(spent), currency(Math.max(0, limit - spent))]
      })
    } else if (type === 'Savings Report') {
      headers = ['Goal', 'Saved', 'Target', 'Progress']
      rows = data.goals.map((goal) => { const saved = Number(goal.saved ?? goal.currentAmount ?? 0); const target = Number(goal.target ?? goal.targetAmount ?? 0); return [goal.name, currency(saved), currency(target), `${target ? Math.round(saved / target * 100) : 0}%`] })
    } else {
      headers = ['Name', 'Category', 'Frequency', 'Amount', 'Next Renewal', 'Status']
      rows = data.subscriptions.map((item) => [item.name, item.category, item.frequency, currency(item.amount), item.renewal, item.status])
    }
    const periodLabel = { month: 'This Month', last: 'Last Month', six: 'Last 6 Months', year: 'This Year', custom: 'Custom Range' }[period]
    return { range, valid, transactions, income, expense, net, headers, rows, periodLabel }
  }, [data, from, period, to, type])
  function exportCsv() {
    if (!report.valid) return
    const rows = [['Report Type', type], ['Period', report.periodLabel], ['From', report.range.start], ['To', report.range.end], ['Total Income', report.income], ['Total Expenses', report.expense], ['Net Savings', report.net], [], report.headers, ...report.rows, [], ['Date', 'Description', 'Category', 'Type', 'Amount']]
    report.transactions.slice().sort((a, b) => String(b.transactionDate).localeCompare(String(a.transactionDate))).forEach((item) => rows.push([item.transactionDate, item.description, item.category?.name || 'Other', item.type, item.amount]))
    downloadCsv('finly-financial-report.csv', rows)
  }
  return <div className="module-page reports-page">
    <header className="page-heading"><div><p className="page-eyebrow">Financial overview</p><h1>Reports</h1><p className="page-description">Generate and review reports from your account data.</p></div></header>
    <section className="module-panel report-controls" aria-label="Report options">
      <label>Report<select value={type} onChange={(event) => setType(event.target.value)}>{types.map((option) => <option key={option}>{option}</option>)}</select></label>
      <label>Period<select value={period} onChange={(event) => setPeriod(event.target.value)}><option value="month">This Month</option><option value="last">Last Month</option><option value="six">Last 6 Months</option><option value="year">This Year</option><option value="custom">Custom Range</option></select></label>
      {period === 'custom' && <><label>From<input type="date" value={from} onChange={(event) => setFrom(event.target.value)} /></label><label>To<input type="date" value={to} onChange={(event) => setTo(event.target.value)} /></label></>}
      <button type="button" className="module-button" onClick={() => setAttempt((value) => value + 1)} disabled={loading}>Generate Report</button>
      <button type="button" className="module-secondary" onClick={() => window.print()} disabled={!report.valid || loading}>Export PDF</button>
      <button type="button" className="module-secondary" onClick={exportCsv} disabled={!report.valid || loading}>Export CSV</button>
    </section>
    {loading ? <section className="module-state" role="status">Loading your account reports…</section> : error ? <section className="module-state" role="alert"><h2>Reports unavailable</h2><p>{error}</p><button type="button" className="module-button" onClick={() => setAttempt((value) => value + 1)}>Try again</button></section> : <>
      {data.partialError && <p className="module-inline-notice" role="status">{data.partialError}</p>}
      {!report.valid && <p className="module-inline-error" role="alert">Choose a valid start and end date.</p>}
      <section className="report-stats"><article><span>Total Income</span><strong>{currency(report.income)}</strong><small>Selected period</small></article><article><span>Total Expenses</span><strong>{currency(report.expense)}</strong><small>Selected period</small></article><article><span>Net Savings</span><strong>{currency(report.net)}</strong><small>Income less expenses</small></article><article><span>Savings Rate</span><strong>{report.income ? `${Math.round(report.net / report.income * 100)}%` : '0%'}</strong><small>Of total income</small></article></section>
      <article className="module-panel report-preview" id="printable-report"><header className="report-document-heading"><div><p className="page-eyebrow">Finly account report</p><h2>{type}</h2><p>Period: {report.periodLabel} · {report.range.start || '—'} to {report.range.end || '—'}</p></div><small>Generated {new Date().toLocaleDateString('en-IN')}</small></header>
        {type === 'Monthly Financial Summary' && <div className="report-summary-row"><div>Total Income<strong>{currency(report.income)}</strong></div><div>Total Expenses<strong>{currency(report.expense)}</strong></div><div>Net Savings<strong>{currency(report.net)}</strong></div></div>}
        <h3>{type === 'Budget Report' ? 'Budget Performance' : type === 'Savings Report' ? 'Savings Goals' : type === 'Subscription Report' ? 'Subscriptions' : type === 'Income Report' ? 'Income by category' : 'Expense Breakdown'}</h3>
        {report.rows.length ? <div className="report-table-scroll"><table className="module-table"><thead><tr>{report.headers.map((header) => <th key={header}>{header}</th>)}</tr></thead><tbody>{report.rows.map((row, index) => <tr key={`${row[0]}-${index}`}>{row.map((cell, cellIndex) => <td key={`${cell}-${cellIndex}`}>{cell}</td>)}</tr>)}</tbody></table></div> : <p className="report-empty">No data for this report period.</p>}
        <div className="report-transactions"><h3>Transaction Summary</h3><p>{report.transactions.length} transaction{report.transactions.length === 1 ? '' : 's'} in the selected period</p>{report.transactions.length ? <div className="report-table-scroll"><table className="module-table"><thead><tr><th>Date</th><th>Description</th><th>Category</th><th>Type</th><th>Amount</th></tr></thead><tbody>{report.transactions.slice().sort((a, b) => String(b.transactionDate).localeCompare(String(a.transactionDate))).map((item) => <tr key={item.id}><td>{showDate(item.transactionDate)}</td><td>{item.description}</td><td>{item.category?.name || 'Other'}</td><td>{typeOf(item, 'INCOME') ? 'Income' : 'Expense'}</td><td>{currency(item.amount)}</td></tr>)}</tbody></table></div> : <p className="report-empty">No transactions for this period.</p>}</div>
      </article>
    </>}
  </div>
}
