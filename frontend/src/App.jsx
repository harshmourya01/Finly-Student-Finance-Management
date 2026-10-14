import { Navigate, Route, Routes } from 'react-router-dom'
import AppLayout from './layouts/AppLayout.jsx'
import AuthPage from './pages/AuthPage.jsx'
import DashboardPage from './pages/DashboardPage.jsx'
import ModulePage from './pages/ModulePage.jsx'
import RequireAuth from './components/RequireAuth.jsx'
import MoneyPage from './pages/MoneyPage.jsx'
import BudgetPage from './pages/BudgetPage.jsx'
import SavingsGoalsPage from './pages/SavingsGoalsPage.jsx'
import SubscriptionsPage from './pages/SubscriptionsPage.jsx'
import AnalysisPage from './pages/AnalysisPage.jsx'
import ReportsPage from './pages/ReportsPage.jsx'
import SettingsPage from './pages/SettingsPage.jsx'

const modules = [
  { path: '/', title: 'Dashboard', description: 'Your personal finance overview.' },
  { path: 'money', title: 'Money / Transactions', description: 'Review and manage income and expenses.' },
  { path: 'budget', title: 'Budget', description: 'Plan budgets and monitor spending.' },
  { path: 'savings-goals', title: 'Savings Goals', description: 'Keep track of progress toward your goals.' },
  { path: 'subscriptions', title: 'Subscriptions', description: 'Review recurring payments and renewals.' },
  { path: 'analysis', title: 'Analysis', description: 'Explore insights about your finances.' },
  { path: 'reports', title: 'Reports', description: 'Review your financial reports.' },
  { path: 'settings', title: 'Settings', description: 'Manage your account preferences.' },
]

function NotFoundPage() {
  return <ModulePage title="Page not found" description="The requested Finly page does not exist." />
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<AuthPage key="login" mode="login" />} />
      <Route path="/register" element={<AuthPage key="register" mode="register" />} />
      <Route element={<RequireAuth />}>
        <Route element={<AppLayout />}>
          {modules.map(({ path, title, description }) => (
            <Route key={path || 'dashboard'} index={path === '/'} path={path === '/' ? undefined : path}
              element={path === '/' ? <DashboardPage /> : path === 'money' ? <MoneyPage /> : path === 'budget' ? <BudgetPage /> : path === 'savings-goals' ? <SavingsGoalsPage /> : path === 'subscriptions' ? <SubscriptionsPage /> : path === 'analysis' ? <AnalysisPage /> : path === 'reports' ? <ReportsPage /> : path === 'settings' ? <SettingsPage /> : <ModulePage title={title} description={description} />} />
          ))}
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
