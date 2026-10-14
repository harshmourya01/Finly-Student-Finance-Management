import { Link } from 'react-router-dom'
import { currency } from '../../utils/dashboard.js'

const budgetIcons = { Food: '🍜', Transportation: '🚌', Shopping: '🛍', Entertainment: '🎬', Education: '📚', Bills: '◉', Healthcare: '✚' }
const goalIcons = ['💻', '🛟', '📱']
const progressTones = ['', 'blue-progress', 'purple-progress', 'orange-progress', 'green-progress']

export function BudgetOverview({ budgets, hasError }) {
  return (
    <article className="dashboard-panel overview-panel">
      <div className="panel-heading"><div><h2>Budget Overview</h2><p>Track your monthly limits</p></div><Link to="/budget">View all <span aria-hidden="true">→</span></Link></div>
      {!budgets.length
        ? <p className="dashboard-empty overview-empty">{hasError ? 'Budget data unavailable.' : 'No budgets for this month.'}</p>
        : <div className="budget-list">{budgets.slice(0, 4).map((budget, index) => {
          const limit = Number(budget.limit ?? budget.amount) || 0
          const spent = Number(budget.spent) || 0
          const percent = limit ? Math.round(spent / limit * 100) : 0
          const name = budget.category || 'Other'
          return <div className="budget-row" key={budget.id ?? `${name}-${index}`}>
            <div className="budget-row-heading"><span className="category-icon">{budgetIcons[name] || '•'}</span><div><strong>{name}</strong><small>{currency(spent)} / {currency(limit)}</small></div><b>{percent}%</b></div>
            <div className={`progress-track ${progressTones[index % progressTones.length]}`}><i style={{ width: `${Math.min(percent, 100)}%` }} /></div>
          </div>
        })}</div>}
    </article>
  )
}

export function SavingsGoalsOverview({ goals, hasError }) {
  return (
    <article className="dashboard-panel overview-panel">
      <div className="panel-heading"><div><h2>Savings Goals</h2><p>Your progress</p></div><Link to="/savings-goals">View all <span aria-hidden="true">→</span></Link></div>
      {!goals.length
        ? <p className="dashboard-empty overview-empty">{hasError ? 'Savings goals unavailable.' : 'No savings goals yet.'}</p>
        : goals.slice(0, 3).map((goal, index) => {
          const saved = Number(goal.saved ?? goal.currentAmount) || 0
          const target = Number(goal.target ?? goal.targetAmount) || 0
          const percent = target ? Math.round(saved / target * 100) : 0
          return <div className="goal-card" key={goal.id ?? goal.name}>
            <span className={`goal-icon${index ? ' mint' : ''}`} aria-hidden="true">{goalIcons[index % goalIcons.length]}</span>
            <div className="goal-data"><div className="goal-title"><strong>{goal.name}</strong><b>{percent}%</b></div>
              <small>{currency(saved)} <span>/ {currency(target)}</span></small>
              <div className="progress-track purple-progress"><i style={{ width: `${Math.min(percent, 100)}%` }} /></div>
            </div>
          </div>
        })}
    </article>
  )
}
