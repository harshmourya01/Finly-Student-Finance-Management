import { useEffect, useRef, useState } from 'react'

const colors = Array.from({ length: 7 }, (_, index) => `var(--analysis-chart-${index + 1})`)

const formatAmount = (value) => `₹${Number(value || 0).toLocaleString('en-IN', { maximumFractionDigits: 2 })}`
const niceStep = (value) => {
  if (value <= 0) return 1
  const power = 10 ** Math.floor(Math.log10(value))
  const fraction = value / power
  return (fraction <= 1 ? 1 : fraction <= 2 ? 2 : fraction <= 5 ? 5 : 10) * power
}

export function TrendChart({ points, granularity = 'month' }) {
  const chartRef = useRef(null)
  const [width, setWidth] = useState(720)
  useEffect(() => {
    const element = chartRef.current
    if (!element) return undefined
    const update = () => setWidth(Math.max(280, Math.floor(element.clientWidth)))
    update()
    const observer = new ResizeObserver(update)
    observer.observe(element)
    return () => observer.disconnect()
  }, [])
  const max = Math.max(0, ...points.map((point) => Number(point.value) || 0))
  if (!max) return <p className="analysis-chart-empty">No spending in this period.</p>
  const left = 62; const right = width - 12; const top = 18; const bottom = 194
  const step = niceStep(max / 4); const ceiling = Math.max(step, Math.ceil(max / step) * step)
  const yTicks = Array.from({ length: Math.round(ceiling / step) + 1 }, (_, index) => index * step)
  const coords = points.map((point, index) => {
    const x = points.length === 1 ? (left + right) / 2 : left + index * (right - left) / (points.length - 1)
    const y = bottom - (Number(point.value || 0) / ceiling) * (bottom - top)
    return { ...point, x, y }
  })
  const labelIndexes = granularity === 'day'
    ? points.map((point, index) => ({ point, index })).filter(({ point, index }) => index === 0 || index === points.length - 1 || Number(point.day) === 1 || Number(point.day) % 5 === 0).map(({ index }) => index)
    : points.map((_, index) => index)
  return <div className="analysis-trend-wrap" ref={chartRef}>
    <svg className="analysis-trend" viewBox={`0 0 ${width} 232`} role="img" aria-label={`${granularity === 'day' ? 'Daily' : 'Monthly'} spending trend, with rupee values on the vertical axis`}>
      {yTicks.map((value) => {
        const y = bottom - value / ceiling * (bottom - top)
        return <g key={value}><line x1={left} x2={right} y1={y} y2={y} className="analysis-gridline" /><text x={left - 10} y={y + 4} textAnchor="end" className="analysis-axis-label">{formatAmount(value)}</text></g>
      })}
      <line x1={left} x2={left} y1={top} y2={bottom} className="analysis-axis" />
      <line x1={left} x2={right} y1={bottom} y2={bottom} className="analysis-axis" />
      <polyline points={coords.map(({ x, y }) => `${x},${y}`).join(' ')} className="analysis-trend-line" />
      {coords.map(({ label, tooltipLabel, value, x, y, day }) => <circle key={label} cx={x} cy={y} r={granularity === 'day' ? 3.2 : 4} className="analysis-trend-point" tabIndex="0" aria-label={`${tooltipLabel || label}: ${formatAmount(value)}`}>
        <title>{`${tooltipLabel || label}: ${formatAmount(value)}`}</title>
      </circle>)}
      {labelIndexes.map((index) => <text key={points[index].label} x={coords[index].x} y={bottom + 23} textAnchor={index === 0 ? 'start' : index === points.length - 1 ? 'end' : 'middle'} className="analysis-axis-label">{granularity === 'day' ? points[index].day : points[index].label}</text>)}
      <text x={(left + right) / 2} y="229" textAnchor="middle" className="analysis-axis-caption">{granularity === 'day' ? 'Day of month' : 'Month'}</text>
    </svg>
  </div>
}

export function CategoryChart({ categories, total }) {
  if (!total) return <p className="analysis-chart-empty">No expenses in this period.</p>
  let offset = 0
  const segments = categories.map(([name, value], index) => {
    const start = offset; offset += value / total
    return { name, value, color: colors[index % colors.length], start, end: offset }
  })
  const polar = (ratio) => { const angle = ratio * Math.PI * 2 - Math.PI / 2; return [50 + Math.cos(angle) * 38, 50 + Math.sin(angle) * 38] }
  const arc = ({ start, end }) => {
    if (end - start >= .999) return 'M 50 12 A 38 38 0 1 1 49.99 12 Z'
    const [sx, sy] = polar(start); const [ex, ey] = polar(end)
    return `M ${sx} ${sy} A 38 38 0 ${end - start > .5 ? 1 : 0} 1 ${ex} ${ey}`
  }
  return <div className="analysis-category-layout"><svg viewBox="0 0 100 100" className="analysis-donut" role="img" aria-label="Expenses by category">
    {segments.map((segment) => <path key={segment.name} d={arc(segment)} fill="none" stroke={segment.color} strokeWidth="12" />)}
    <text x="50" y="47" textAnchor="middle" className="analysis-donut-value">₹{Math.round(total).toLocaleString('en-IN')}</text><text x="50" y="59" textAnchor="middle" className="analysis-donut-caption">Expenses</text>
  </svg><div className="analysis-category-list">{segments.map((segment) => <div key={segment.name}><i style={{ background: segment.color }} /><span>{segment.name}</span><strong>₹{Math.round(segment.value).toLocaleString('en-IN')}</strong></div>)}</div></div>
}

export function IncomeExpenseChart({ months }) {
  const max = Math.max(0, ...months.flatMap((month) => [month.income, month.expense]))
  if (!max) return <p className="analysis-chart-empty">No income or expenses in this period.</p>
  return <div className="analysis-bars">{months.map((month) => <div className="analysis-month-bars" key={month.key} aria-label={`${month.label}: income ₹${month.income}, expenses ₹${month.expense}`}>
    <div className="analysis-bar-pair"><i title={`Income ₹${Math.round(month.income).toLocaleString('en-IN')}`} style={{ height: `${month.income ? Math.max(3, month.income / max * 100) : 0}%` }} /><i title={`Expenses ₹${Math.round(month.expense).toLocaleString('en-IN')}`} style={{ height: `${month.expense ? Math.max(3, month.expense / max * 100) : 0}%` }} /></div><span>{month.label}</span>
  </div>)}</div>
}
