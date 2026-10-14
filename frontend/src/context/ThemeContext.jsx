import { createContext, useCallback, useContext, useEffect, useState } from 'react'

const ThemeContext = createContext(null)
const validTheme = (value) => value === 'light' || value === 'dark'

function initialTheme() {
  try {
    const saved = localStorage.getItem('finly-theme')
    if (validTheme(saved)) return saved
  } catch { /* Storage can be unavailable in restricted browser contexts. */ }
  return 'dark'
}

export function ThemeProvider({ children }) {
  const [theme, setTheme] = useState(initialTheme)
  useEffect(() => {
    document.documentElement.dataset.theme = theme
    document.documentElement.style.colorScheme = theme
    try { localStorage.setItem('finly-theme', theme) } catch { /* Theme still applies for this session. */ }
    const meta = document.querySelector('meta[name="theme-color"]')
    if (meta) meta.content = theme === 'dark' ? '#101411' : '#f3f6f2'
  }, [theme])
  const toggleTheme = useCallback(() => setTheme((value) => value === 'dark' ? 'light' : 'dark'), [])
  const value = { theme, setTheme: (next) => { if (validTheme(next)) setTheme(next) }, toggleTheme }
  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}

export function useTheme() {
  const context = useContext(ThemeContext)
  if (!context) throw new Error('useTheme must be used within ThemeProvider')
  return context
}
