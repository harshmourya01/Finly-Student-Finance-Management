import { useState } from 'react'
import DashboardToolbar from '../components/dashboard/DashboardToolbar.jsx'
import DashboardSummary from '../components/dashboard/DashboardSummary.jsx'
import IncomeExpenseChart from '../components/dashboard/IncomeExpenseChart.jsx'
import ExpenseBreakdown from '../components/dashboard/ExpenseBreakdown.jsx'
import { BudgetOverview, SavingsGoalsOverview } from '../components/dashboard/OverviewPanels.jsx'
import RecentTransactions from '../components/dashboard/RecentTransactions.jsx'
import useDashboardData from '../hooks/useDashboardData.js'
import { getPeriodRange } from '../utils/dashboard.js'
import '../styles/dashboard.css'

const periodLabels = { month: 'This Month', last: 'Last Month', six: 'Last 6 months', year: 'This Year' }

export default function DashboardPage() {
  const dashboard = useDashboardData()
  const [period, setPeriod] = useState('month')
  const [chartPeriod, setChartPeriod] = useState('six')
  const range = getPeriodRange(period)

  return (
    <div className="dashboard-page">
      <section className="dashboard-page-heading">
        <div><p className="dashboard-eyebrow">Overview</p><h1>Dashboard</h1><p className="dashboard-intro">Here's your financial overview.</p></div>
        <DashboardToolbar period={period} setPeriod={setPeriod} />
      </section>
      {dashboard.errors.dashboard && <div className="dashboard-notice" role="alert">{dashboard.errors.dashboard}</div>}
      {dashboard.loading && <div className="dashboard-loading" role="status">Loading your financial overview…</div>}
      <DashboardSummary balance={dashboard.balance} transactions={dashboard.transactions} range={range} periodLabel={periodLabels[period]} errors={dashboard.errors} />
      <section className="dashboard-chart-grid" aria-label="Financial charts">
        <IncomeExpenseChart transactions={dashboard.transactions} period={chartPeriod} setPeriod={setChartPeriod} hasError={Boolean(dashboard.errors.transactions)} />
        <ExpenseBreakdown transactions={dashboard.transactions} range={range} hasError={Boolean(dashboard.errors.transactions)} />
      </section>
      <section className="dashboard-overview-grid" aria-label="Budgets and savings goals">
        <BudgetOverview budgets={dashboard.budgets} hasError={Boolean(dashboard.errors.budgets)} />
        <SavingsGoalsOverview goals={dashboard.goals} hasError={Boolean(dashboard.errors.goals)} />
      </section>
      <RecentTransactions transactions={dashboard.transactions} hasError={Boolean(dashboard.errors.transactions)} />
    </div>
  )
}
