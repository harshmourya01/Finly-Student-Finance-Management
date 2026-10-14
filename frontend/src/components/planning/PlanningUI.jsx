export function PageHeading({ eyebrow, title, description, action }) {
  return <header className="planning-heading"><div><p className="planning-eyebrow">{eyebrow}</p><h1>{title}</h1><p className="planning-description">{description}</p></div>{action}</header>
}

export function Message({ children, onRetry, tone = 'error' }) {
  if (!children) return null
  return <div className={`planning-message ${tone}`} role={tone === 'error' ? 'alert' : 'status'}><span>{children}</span>{onRetry && <button type="button" onClick={onRetry}>Try again</button>}</div>
}

export function EmptyState({ title, children, action }) {
  return <section className="planning-empty"><span aria-hidden="true">✦</span><h2>{title}</h2><p>{children}</p>{action}</section>
}

export function Modal({ title, eyebrow, onClose, onSubmit, busy, children, submitLabel = 'Save' }) {
  return <div className="planning-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget && !busy) onClose() }}>
    <section className="planning-modal" role="dialog" aria-modal="true" aria-label={title}>
      <header className="planning-modal-heading"><div><p className="planning-eyebrow">{eyebrow}</p><h2>{title}</h2></div><button type="button" className="planning-close" aria-label="Close" onClick={onClose} disabled={busy}>×</button></header>
      <form onSubmit={onSubmit}><div className="planning-form-grid">{children}</div><footer className="planning-modal-actions"><button type="button" className="planning-secondary" onClick={onClose} disabled={busy}>Cancel</button><button className="planning-primary" disabled={busy}>{busy ? 'Saving…' : submitLabel}</button></footer></form>
    </section>
  </div>
}

export function Field({ label, children, wide = false }) {
  return <label className={`planning-field${wide ? ' wide' : ''}`}><span>{label}</span>{children}</label>
}

export function LoadingState({ children = 'Loading…' }) {
  return <section className="planning-empty" role="status"><p>{children}</p></section>
}
