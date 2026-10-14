export const currency = (value) => new Intl.NumberFormat('en-IN', {
  style: 'currency', currency: 'INR', maximumFractionDigits: 0,
}).format(Number(value) || 0)

export const dateKey = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`

const monthKey = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
const shiftedMonth = (date, amount) => new Date(date.getFullYear(), date.getMonth() + amount, 1)

export function getPeriodRange(key, now = new Date()) {
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  if (key === 'last') {
    const start = shiftedMonth(today, -1)
    return { start: dateKey(start), end: dateKey(new Date(today.getFullYear(), today.getMonth(), 0)) }
  }
  if (key === 'six') return { start: dateKey(shiftedMonth(today, -5)), end: dateKey(today) }
  if (key === 'year') return { start: `${today.getFullYear()}-01-01`, end: dateKey(today) }
  return { start: dateKey(new Date(today.getFullYear(), today.getMonth(), 1)), end: dateKey(today) }
}

export function sumTransactions(items) {
  return items.reduce((sum, item) => {
    const amount = Number(item.amount) || 0
    if (String(item.type).toUpperCase() === 'INCOME') sum.income += amount
    if (String(item.type).toUpperCase() === 'EXPENSE') sum.expenses += amount
    return sum
  }, { income: 0, expenses: 0 })
}

export function getMonthlySeries(transactions, period, now = new Date()) {
  const count = period === 'year' ? now.getMonth() + 1 : 6
  const first = period === 'year' ? new Date(now.getFullYear(), 0, 1) : shiftedMonth(new Date(now.getFullYear(), now.getMonth(), 1), -5)
  return Array.from({ length: count }, (_, index) => {
    const date = shiftedMonth(first, index)
    const key = monthKey(date)
    const total = sumTransactions(transactions.filter((item) => String(item.transactionDate).startsWith(key)))
    return { label: date.toLocaleDateString('en-IN', { month: 'short' }), ...total }
  })
}

export function getExpenseCategories(transactions, range) {
  const totals = new Map()
  transactions
    .filter((item) => String(item.type).toUpperCase() === 'EXPENSE'
      && item.transactionDate >= range.start && item.transactionDate <= range.end)
    .forEach((item) => {
      const category = item.category?.name || 'Other'
      totals.set(category, (totals.get(category) || 0) + (Number(item.amount) || 0))
    })
  return [...totals.entries()].map(([name, amount]) => ({ name, amount })).sort((a, b) => b.amount - a.amount)
}

export function getNotifications({ subscriptions, budgets, goals }) {
  const notices = []
  const today = new Date()
  today.setHours(0, 0, 0, 0)

  subscriptions.filter((item) => String(item.status).toUpperCase() === 'ACTIVE').forEach((item) => {
    const renewalDate = item.renewal ? new Date(`${item.renewal}T00:00:00`) : null
    if (!renewalDate || Number.isNaN(renewalDate.getTime())) return
    const days = Math.ceil((renewalDate - today) / 86_400_000)
    if (days >= 0 && days <= 7) {
      notices.push({ title: 'Subscription renewal', text: `${item.name} renews ${days === 0 ? 'today' : days === 1 ? 'tomorrow' : `in ${days} days`}.` })
    }
  })

  budgets.forEach((item) => {
    const limit = Number(item.limit ?? item.amount) || 0
    const spent = Number(item.spent) || 0
    if (limit > 0 && spent / limit >= 0.8) {
      notices.push({ title: spent > limit ? 'Budget exceeded' : 'Budget nearing limit', text: `${item.category || 'A category'} is at ${Math.round(spent / limit * 100)}% of its budget.` })
    }
  })

  goals.forEach((item) => {
    const target = Number(item.target ?? item.targetAmount) || 0
    const saved = Number(item.saved ?? item.currentAmount) || 0
    if (target > 0 && saved / target >= 0.8) {
      notices.push({ title: saved >= target ? 'Savings goal reached' : 'Savings goal nearly reached', text: `${item.name} is ${Math.round(saved / target * 100)}% complete.` })
    }
  })
  return notices
}
