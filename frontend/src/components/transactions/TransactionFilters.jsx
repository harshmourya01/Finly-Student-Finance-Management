const paymentMethods = [
  ['UPI', 'UPI'], ['CASH', 'Cash'], ['CREDIT_CARD', 'Credit Card'],
  ['DEBIT_CARD', 'Debit Card'], ['BANK_TRANSFER', 'Bank Transfer'], ['OTHER', 'Other'],
]

export default function TransactionFilters({ categories, filters, onChange, onClear }) {
  const names = [...new Set(categories.map((category) => category.name))].sort((a, b) => a.localeCompare(b))
  return (
    <section className="money-filter-bar" aria-label="Filter transactions">
      <label className="money-search-field">
        <span aria-hidden="true">⌕</span>
        <input type="search" value={filters.search} onChange={(event) => onChange('search', event.target.value)} placeholder="Search transactions" aria-label="Search transactions" />
      </label>
      <label className="money-filter-label">
        <span className="sr-only">Category</span>
        <select value={filters.category} onChange={(event) => onChange('category', event.target.value)} aria-label="Category">
          <option value="">All categories</option>{names.map((name) => <option key={name} value={name}>{name}</option>)}
        </select>
      </label>
      <label className="money-filter-label">
        <span className="sr-only">Date</span>
        <select value={filters.date} onChange={(event) => onChange('date', event.target.value)} aria-label="Date">
          <option value="">All dates</option><option value="this-month">This month</option><option value="last-month">Last month</option>
        </select>
      </label>
      <label className="money-filter-label">
        <span className="sr-only">Payment method</span>
        <select value={filters.method} onChange={(event) => onChange('method', event.target.value)} aria-label="Payment method">
          <option value="">All payment methods</option>{paymentMethods.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select>
      </label>
      <label className="money-amount-filter">
        <span className="sr-only">Exact amount</span>
        <input type="number" min="0" step="0.01" value={filters.amount} onChange={(event) => onChange('amount', event.target.value)} placeholder="Amount" aria-label="Exact amount" />
      </label>
      <button className="money-clear-filters" type="button" onClick={onClear}>Clear filters</button>
    </section>
  )
}
