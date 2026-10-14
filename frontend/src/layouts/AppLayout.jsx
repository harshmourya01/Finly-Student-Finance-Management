import { Outlet } from 'react-router-dom'
import Header from '../components/Header.jsx'
import Sidebar from '../components/Sidebar.jsx'

export default function AppLayout() {
  return (
    <div className="app-shell">
      <Sidebar />
      <div className="workspace">
        <Header />
        <main className="main-area">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
