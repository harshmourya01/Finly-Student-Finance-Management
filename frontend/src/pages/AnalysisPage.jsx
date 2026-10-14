import { useMemo, useState } from 'react'
import { CategoryChart, IncomeExpenseChart, TrendChart } from '../components/analysis/Charts.jsx'
import useAnalysis from '../hooks/useAnalysis.js'

const currency = (value) => `₹${Math.round(value || 0).toLocaleString('en-IN')}`
const dateKey = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
const shiftMonth = (date, amount) => new Date(date.getFullYear(), date.getMonth() + amount, 1)
const monthsBetween = (start, end) => {
  const months = []; const cursor = new Date(Number(start.slice(0, 4)), Number(start.slice(5, 7)) - 1, 1)
  const last = new Date(Number(end.slice(0, 4)), Number(end.slice(5, 7)) - 1, 1)
  while (cursor <= last) { months.push(new Date(cursor)); cursor.setMonth(cursor.getMonth() + 1) }
  return months
}
function ranges(key) {
  const now = new Date(); const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  if (key === 'last') return { start: dateKey(shiftMonth(today, -1)), end: dateKey(new Date(today.getFullYear(), today.getMonth(), 0)), note: 'Last month', previous: { start: dateKey(shiftMonth(today, -2)), end: dateKey(new Date(today.getFullYear(), today.getMonth() - 1, 0)) } }
  if (key === 'six') return { start: dateKey(shiftMonth(today, -5)), end: dateKey(today), note: 'Last 6 months', previous: { start: dateKey(shiftMonth(today, -11)), end: dateKey(new Date(today.getFullYear(), today.getMonth() - 5, 0)) } }
  if (key === 'year') return { start: `${today.getFullYear()}-01-01`, end: dateKey(today), note: 'This year', previous: { start: `${today.getFullYear() - 1}-01-01`, end: `${today.getFullYear() - 1}-12-31` } }
  return { start: dateKey(new Date(today.getFullYear(), today.getMonth(), 1)), end: dateKey(today), note: 'This month', previous: { start: dateKey(shiftMonth(today, -1)), end: dateKey(new Date(today.getFullYear(), today.getMonth(), 0)) } }
}
const isType = (item, type) => String(item.type).toUpperCase() === type
const totalOf = (items, type) => items.filter((item) => isType(item, type)).reduce((sum, item) => sum + Number(item.amount || 0), 0)

export default function AnalysisPage() {
  const [period, setPeriod] = useState('month')
  const { transactions, loading, error, retry } = useAnalysis()
  const data = useMemo(() => {
    const range = ranges(period)
    const selected = transactions.filter((item) => item.transactionDate >= range.start && item.transactionDate <= range.end)
    const expenses = selected.filter((item) => isType(item, 'EXPENSE'))
    const income = totalOf(selected, 'INCOME'); const spending = totalOf(selected, 'EXPENSE')
    const days = Math.max(1, Math.floor((new Date(`${range.end}T00:00:00`) - new Date(`${range.start}T00:00:00`)) / 86400000) + 1)
    const categoryMap = new Map()
    expenses.forEach((item) => { const name = item.category?.name || 'Other'; categoryMap.set(name, (categoryMap.get(name) || 0) + Number(item.amount || 0)) })
    const categories = [...categoryMap.entries()].sort((a, b) => b[1] - a[1])
    const previousSpending = totalOf(transactions.filter((item) => item.transactionDate >= range.previous.start && item.transactionDate <= range.previous.end), 'EXPENSE')
    const change = previousSpending ? Math.round((spending - previousSpending) / previousSpending * 100) : spending ? 100 : 0
    const monthList = monthsBetween(range.start, range.end).map((month) => {
      const key = `${month.getFullYear()}-${String(month.getMonth() + 1).padStart(2, '0')}`
      const inMonth = selected.filter((item) => item.transactionDate?.startsWith(key))
      return { key, label: month.toLocaleDateString('en-IN', { month: 'short' }), tooltipLabel: month.toLocaleDateString('en-IN', { month: 'long', year: 'numeric' }), expense: totalOf(inMonth, 'EXPENSE'), income: totalOf(inMonth, 'INCOME') }
    })
    const trendGranularity = period === 'month' || period === 'last' ? 'day' : 'month'
    const trendPoints = trendGranularity === 'day'
      ? (() => {
        const dailyExpenses = new Map()
        selected.filter((item) => isType(item, 'EXPENSE')).forEach((item) => {
          dailyExpenses.set(item.transactionDate, (dailyExpenses.get(item.transactionDate) || 0) + Number(item.amount || 0))
        })
        const dates = []; const cursor = new Date(`${range.start}T00:00:00`); const endDate = new Date(`${range.end}T00:00:00`)
        while (cursor <= endDate) {
          const key = dateKey(cursor)
          dates.push({ label: key, day: cursor.getDate(), tooltipLabel: cursor.toLocaleDateString('en-IN', { day: 'numeric', month: 'long', year: 'numeric' }), value: dailyExpenses.get(key) || 0 })
          cursor.setDate(cursor.getDate() + 1)
        }
        return dates
      })()
      : monthList.map((item) => ({ label: item.key, day: undefined, tooltipLabel: item.tooltipLabel, value: item.expense }))
    const methodTotals = new Map()
    selected.forEach((item) => { const method = String(item.paymentMethod || 'Other').replaceAll('_', ' '); methodTotals.set(method, (methodTotals.get(method) || 0) + Number(item.amount || 0)) })
    const methods = [...methodTotals.entries()].sort((a, b) => b[1] - a[1]); const methodsTotal = methods.reduce((sum, [, value]) => sum + value, 0)
    return { ...range, selected, income, spending, days, categories, change, monthList, trendPoints, trendGranularity, methods, methodsTotal }
  }, [period, transactions])
  return <div className="module-page analysis-page">
    <header className="page-heading"><div><p className="page-eyebrow">Insights</p><h1>Analysis</h1><p className="page-description">Understand your income and spending patterns.</p></div><label className="analysis-period">Period<select value={period} onChange={(event) => setPeriod(event.target.value)}><option value="month">This Month</option><option value="last">Last Month</option><option value="six">Last 6 Months</option><option value="year">This Year</option></select></label></header>
    {error ? <section className="module-state" role="alert"><h2>Analysis unavailable</h2><p>{error}</p><button type="button" className="module-button" onClick={retry}>Try again</button></section> : loading ? <section className="module-state" role="status">Loading your analysis…</section> : <>
      <section className="analysis-stats"><article><span>Total Income</span><strong>{currency(data.income)}</strong><small>{data.note}</small></article><article><span>Total Spending</span><strong>{currency(data.spending)}</strong><small>{data.note}</small></article><article><span>Average Daily Spending</span><strong>{currency(data.spending / data.days)}</strong><small>Based on selected period</small></article><article><span>Change vs Previous Period</span><strong className={data.change > 0 ? 'analysis-up' : 'analysis-down'}>{data.change > 0 ? '+' : ''}{data.change}%</strong><small>Spending comparison</small></article></section>
      {transactions.length === 0 ? <section className="module-state"><h2>No transactions yet</h2><p>Once you record income or expenses, your financial analysis will appear here.</p></section> : <>
        <section className="analysis-panels"><article className="module-panel"><div className="module-panel-heading"><div><h2>Spending Trend</h2><p>{data.trendGranularity === 'day' ? 'Daily expense activity' : 'Monthly expense activity'}</p></div></div><TrendChart points={data.trendPoints} granularity={data.trendGranularity} /></article><article className="module-panel"><div className="module-panel-heading"><div><h2>Expense Distribution</h2><p>Spending by category</p></div></div><CategoryChart categories={data.categories} total={data.spending} /></article></section>
        <section className="analysis-panels analysis-second-row"><article className="module-panel"><div className="module-panel-heading"><div><h2>Income vs Expenses</h2><p>Monthly comparison</p></div></div><IncomeExpenseChart months={data.monthList} /><div className="analysis-legend"><span><i className="income-color" />Income</span><span><i className="expense-color" />Expenses</span></div></article><article className="module-panel"><div className="module-panel-heading"><div><h2>Payment Methods</h2><p>Transaction amounts by method</p></div></div>{data.methodsTotal ? <div className="analysis-methods">{data.methods.map(([method, value], index) => <div key={method}><div><span>{method}</span><b>{Math.round(value / data.methodsTotal * 100)}%</b></div><div className="analysis-method-track"><i style={{ width: `${value / data.methodsTotal * 100}%`, background: ['#c9f28a', '#83b5f7', '#c49af5', '#f0b560'][index % 4] }} /></div></div>)}</div> : <p className="analysis-chart-empty">No transactions in this period.</p>}</article></section>
      </>}
    </>}
  </div>
}
