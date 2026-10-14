export default function ModulePage({ title, description }) {
  return (
    <>
      <header className="page-heading">
        <p className="page-eyebrow">Finly workspace</p>
        <h1>{title}</h1>
        <p className="page-description">{description}</p>
      </header>
      <section className="empty-panel rounded-xl border border-slate-700" aria-label={`${title} placeholder`}>
        <div>
          <span className="empty-icon" aria-hidden="true">·</span>
          <h2>{title} is ready for setup</h2>
          <p>This page is a clean starting point. Its existing project functionality will be connected in a later migration phase.</p>
        </div>
      </section>
    </>
  )
}
