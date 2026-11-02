import { FormEvent, useEffect, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { ApiError, api } from './api'
import { BandMeter, Banner } from './components'
import { bandLabel, compa, count, money, titleCase, usd } from './format'
import type { Analytics, AuditPage, EmployeeDetail, EmployeePage, Meta } from './types'

function useMeta() {
  const [meta, setMeta] = useState<Meta | null>(null)
  useEffect(() => {
    api<Meta>('/api/reference/meta').then(setMeta).catch(() => setMeta(null))
  }, [])
  return meta
}

function problem(error: unknown): string[] {
  if (error instanceof ApiError) {
    if (error.payload.violations?.length) return error.payload.violations
    if (error.payload.fields && Object.keys(error.payload.fields).length) {
      return Object.entries(error.payload.fields).map(([field, message]) => `${field} ${message}`)
    }
    return [error.payload.message || 'Request failed']
  }
  return ['Something went wrong.']
}

export function LoginPage({ onSuccess }: { onSuccess: () => void }) {
  const [username, setUsername] = useState('hr.manager')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string[] | null>(null)
  const [busy, setBusy] = useState(false)

  async function submit(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await api('/api/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) })
      onSuccess()
    } catch (caught) {
      setError(problem(caught))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="login-wrap">
      <form className="login card" onSubmit={submit}>
        <div className="mark">
          <strong>ACME</strong>
          <span>Compensation</span>
        </div>
        <h1>Sign in</h1>
        <p className="lede">The HR workspace for how ACME pays its people.</p>
        <Banner error={error} />
        <div className="form-grid column">
          <label className="field">Username
            <input value={username} onChange={(event) => setUsername(event.target.value)} autoComplete="username" />
          </label>
          <label className="field">Password
            <input value={password} onChange={(event) => setPassword(event.target.value)} type="password" autoComplete="current-password" />
          </label>
          <button type="submit" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>
        </div>
        <p className="note">Demo access: hr.manager / Hr-Manager-Demo-2026!. This account is for the local assessment only.</p>
      </form>
    </div>
  )
}

export function DashboardPage() {
  const meta = useMeta()
  const [params, setParams] = useSearchParams()
  const [data, setData] = useState<Analytics | null>(null)
  const [error, setError] = useState<string[] | null>(null)
  const country = params.get('country') ?? ''
  const department = params.get('department') ?? ''

  useEffect(() => {
    const query = new URLSearchParams()
    if (country) query.set('country', country)
    if (department) query.set('department', department)
    api<Analytics>(`/api/analytics/overview?${query.toString()}`)
      .then((next) => { setData(next); setError(null) })
      .catch((caught) => setError(problem(caught)))
  }, [country, department])

  function setFilter(key: string, value: string) {
    const next = new URLSearchParams(params)
    if (value) next.set(key, value)
    else next.delete(key)
    setParams(next)
  }

  return (
    <>
      <div className="page-head">
        <div>
          <h1>How we pay</h1>
          <p>Active employees with pay already in effect. Figures are annual. USD uses the snapshot dated {data?.fxAsOf ?? '—'}.</p>
        </div>
      </div>
      <div className="filters">
        <label className="field">Country
          <select value={country} onChange={(event) => setFilter('country', event.target.value)}>
            <option value="">All countries</option>
            {meta?.countries.map((item) => <option key={item.code} value={item.code}>{item.label}</option>)}
          </select>
        </label>
        <label className="field">Department
          <select value={department} onChange={(event) => setFilter('department', event.target.value)}>
            <option value="">All departments</option>
            {meta?.departments.map((item) => <option key={item.code} value={item.code}>{item.label}</option>)}
          </select>
        </label>
      </div>
      <Banner error={error} />
      {!data ? <p>Loading the pay picture…</p> : (
        <>
          <div className="stats">
            <div className="stat"><span>People in payroll</span><strong>{count(data.activeWithPay)}</strong></div>
            <div className="stat"><span>Annual base payroll</span><strong>{usd(data.pay.totalBaseUsd)}</strong></div>
            <div className="stat"><span>Annual total compensation</span><strong>{usd(data.pay.totalCompUsd)}</strong></div>
            <div className="stat"><span>Median base</span><strong>{usd(data.pay.medianBaseUsd)}</strong></div>
          </div>
          <div className="grid-2">
            <section className="card">
              <h2>Base pay spread</h2>
              <p className="muted">25th {usd(data.pay.p25BaseUsd)} · mean {usd(data.pay.meanBaseUsd)} · 75th {usd(data.pay.p75BaseUsd)}</p>
              <p>{data.pay.outsideBandPercent}% of people are outside their base-salary band.</p>
              <p>
                <span className="pill bad">{count(data.pay.belowBand)} below</span>{' '}
                <span className="pill good">{count(data.pay.inBand)} in band</span>{' '}
                <span className="pill warn">{count(data.pay.aboveBand)} above</span>
              </p>
              <p className="muted">{count(data.formerHeadcount)} former employees are excluded. {count(data.scheduledHeadcount)} active people have only future-dated pay.</p>
            </section>
            <section className="card">
              <h2>Gender, base USD</h2>
              {data.pay.equity.length === 0 ? <p>No group is large enough to show.</p> : (
                <table>
                  <thead><tr><th>Group</th><th>People</th><th>Median</th><th>Mean</th></tr></thead>
                  <tbody>
                    {data.pay.equity.map((row) => (
                      <tr key={row.group}>
                        <td>{row.label}</td>
                        <td>{count(row.headcount)}</td>
                        <td>{usd(row.medianBaseUsd)}</td>
                        <td>{usd(row.meanBaseUsd)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
              <p className="muted">Groups under {data.pay.minimumGroupSize} people are hidden. {data.pay.suppressedGroups} hidden in this view.</p>
            </section>
          </div>
          <div className="stack">
            <BreakdownCard title="By country" rows={data.pay.byCountry} />
            <BreakdownCard title="By department" rows={data.pay.byDepartment} />
            <BreakdownCard title="By level" rows={data.pay.byLevel} />
          </div>
          <ul className="note">
            {data.notes.map((note) => <li key={note}>{note}</li>)}
          </ul>
        </>
      )}
    </>
  )
}

function BreakdownCard({ title, rows }: { title: string; rows: Analytics['pay']['byCountry'] }) {
  const max = Math.max(...rows.map((row) => Number(row.totalBaseUsd)), 1)
  return (
    <section className="card">
      <h2>{title}</h2>
      <table>
        <thead>
          <tr><th>Group</th><th>People</th><th>Base payroll</th><th>Median</th><th>Outside</th></tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.key}>
              <td>{row.label}</td>
              <td>{count(row.headcount)}</td>
              <td>
                {usd(row.totalBaseUsd)}
                <div className="bar" aria-hidden="true"><i style={{ width: `${(Number(row.totalBaseUsd) / max) * 100}%` }} /></div>
              </td>
              <td>{usd(row.medianBaseUsd)}</td>
              <td>{count(row.belowBand + row.aboveBand)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  )
}

export function PeoplePage() {
  const meta = useMeta()
  const [params, setParams] = useSearchParams()
  const [data, setData] = useState<EmployeePage | null>(null)
  const [error, setError] = useState<string[] | null>(null)
  const [draft, setDraft] = useState(params.get('q') ?? '')
  const navigate = useNavigate()

  const query = params.toString()
  useEffect(() => {
    api<EmployeePage>(`/api/employees?${query}`)
      .then((next) => { setData(next); setError(null) })
      .catch((caught) => setError(problem(caught)))
  }, [query])

  function update(key: string, value: string) {
    const next = new URLSearchParams(params)
    if (value) next.set(key, value)
    else next.delete(key)
    if (key !== 'page') next.delete('page')
    setParams(next)
  }

  const page = Number(params.get('page') ?? '0')
  const pages = data ? Math.max(1, Math.ceil(data.total / data.size)) : 1

  return (
    <>
      <div className="page-head">
        <div>
          <h1>People</h1>
          <p>Search the workforce and open a person to see pay history.</p>
        </div>
        <Link className="button" to="/people/new">Add employee</Link>
      </div>
      <form className="filters" onSubmit={(event) => { event.preventDefault(); update('q', draft.trim()) }}>
        <label className="field grow">Search
          <input value={draft} onChange={(event) => setDraft(event.target.value)} placeholder="Name, email, or employee number" />
        </label>
        <label className="field">Country
          <select value={params.get('country') ?? ''} onChange={(event) => update('country', event.target.value)}>
            <option value="">All</option>
            {meta?.countries.map((item) => <option key={item.code} value={item.code}>{item.label}</option>)}
          </select>
        </label>
        <label className="field">Department
          <select value={params.get('department') ?? ''} onChange={(event) => update('department', event.target.value)}>
            <option value="">All</option>
            {meta?.departments.map((item) => <option key={item.code} value={item.code}>{item.label}</option>)}
          </select>
        </label>
        <label className="field">Level
          <select value={params.get('level') ?? ''} onChange={(event) => update('level', event.target.value)}>
            <option value="">All</option>
            {meta?.levels.map((level) => <option key={level} value={level}>{level}</option>)}
          </select>
        </label>
        <label className="field">Status
          <select value={params.get('status') ?? 'ACTIVE'} onChange={(event) => update('status', event.target.value)}>
            <option value="ACTIVE">Active</option>
            <option value="TERMINATED">Former</option>
            <option value="ALL">Everyone</option>
          </select>
        </label>
        <button type="submit">Search</button>
      </form>
      <Banner error={error} />
      <section className="card">
        {!data ? <p>Loading people…</p> : data.items.length === 0 ? <p>No one matches these filters.</p> : (
          <table>
            <thead>
              <tr>
                <th>Employee</th><th>Role</th><th>Location</th><th>Annual base</th><th>USD</th><th>Compa</th><th>Band</th>
              </tr>
            </thead>
            <tbody>
              {data.items.map((person) => (
                <tr key={person.id} className="clickable" onClick={() => navigate(`/people/${person.id}`)}>
                  <td>
                    <Link to={`/people/${person.id}`}><strong>{person.firstName} {person.lastName}</strong></Link>
                    <div className="muted">{person.employeeNumber}</div>
                  </td>
                  <td>{person.jobTitle}<div className="muted">{person.jobLevel} · {person.departmentName}</div></td>
                  <td>{person.countryName}</td>
                  <td>{money(person.baseSalary, person.currency)}</td>
                  <td>{usd(person.baseSalaryUsd)}</td>
                  <td>{compa(person.compaRatio)}</td>
                  <td><span className={pillClass(person.bandPosition)}>{bandLabel(person.bandPosition)}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {data && (
          <div className="pager">
            <span>{count(data.total)} {data.total === 1 ? 'person' : 'people'} · page {page + 1} of {pages}</span>
            <span>
              <button className="secondary" type="button" disabled={page <= 0} onClick={() => update('page', String(page - 1))}>Previous</button>{' '}
              <button className="secondary" type="button" disabled={page + 1 >= pages} onClick={() => update('page', String(page + 1))}>Next</button>
            </span>
          </div>
        )}
      </section>
    </>
  )
}

function pillClass(position: string | null) {
  if (position === 'IN_BAND') return 'pill good'
  if (position === 'ABOVE') return 'pill warn'
  if (position === 'BELOW') return 'pill bad'
  return 'pill'
}

export function NewEmployeePage() {
  const meta = useMeta()
  const navigate = useNavigate()
  const [error, setError] = useState<string[] | null>(null)
  const [busy, setBusy] = useState(false)
  const [form, setForm] = useState({
    firstName: '', lastName: '', email: '', country: 'US', department: 'ENGINEERING',
    jobTitle: '', jobLevel: 'L3', gender: 'UNDISCLOSED', hireDate: '2026-11-02',
    baseSalary: '', annualBonus: '0.00', allowances: '0.00', reason: '', exceptionReason: ''
  })

  function set<K extends keyof typeof form>(key: K, value: string) {
    setForm((current) => ({ ...current, [key]: value }))
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const created = await api<EmployeeDetail>('/api/employees', {
        method: 'POST',
        body: JSON.stringify({
          ...form,
          reason: form.reason || null,
          exceptionReason: form.exceptionReason || null
        })
      })
      navigate(`/people/${created.employee.id}`)
    } catch (caught) {
      setError(problem(caught))
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Add employee</h1>
          <p>The first salary is the hire record. It is effective on the hire date and is not edited later.</p>
        </div>
      </div>
      <form className="card form-grid" onSubmit={submit}>
        <Banner error={error} />
        <Text label="First name" value={form.firstName} onChange={(value) => set('firstName', value)} />
        <Text label="Last name" value={form.lastName} onChange={(value) => set('lastName', value)} />
        <Text label="Email" value={form.email} onChange={(value) => set('email', value)} />
        <Select label="Country" value={form.country} onChange={(value) => set('country', value)} options={meta?.countries ?? []} />
        <Select label="Department" value={form.department} onChange={(value) => set('department', value)} options={meta?.departments ?? []} />
        <Text label="Job title" value={form.jobTitle} onChange={(value) => set('jobTitle', value)} />
        <label className="field">Level
          <select value={form.jobLevel} onChange={(event) => set('jobLevel', event.target.value)}>
            {meta?.levels.map((level) => <option key={level}>{level}</option>)}
          </select>
        </label>
        <Select label="Gender" value={form.gender} onChange={(value) => set('gender', value)} options={meta?.genders ?? []} />
        <Text label="Hire date" value={form.hireDate} onChange={(value) => set('hireDate', value)} type="date" />
        <Text label="Annual base" value={form.baseSalary} onChange={(value) => set('baseSalary', value)} />
        <Text label="Annual bonus" value={form.annualBonus} onChange={(value) => set('annualBonus', value)} />
        <Text label="Annual allowances" value={form.allowances} onChange={(value) => set('allowances', value)} />
        <Text label="Note" value={form.reason} onChange={(value) => set('reason', value)} />
        <Text label="Exception reason" value={form.exceptionReason} onChange={(value) => set('exceptionReason', value)} />
        <button type="submit" disabled={busy}>{busy ? 'Saving…' : 'Create employee'}</button>
      </form>
    </>
  )
}

export function EmployeePage() {
  const { id } = useParams()
  const meta = useMeta()
  const [detail, setDetail] = useState<EmployeeDetail | null>(null)
  const [error, setError] = useState<string[] | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [changeType, setChangeType] = useState('MERIT')
  const [effectiveDate, setEffectiveDate] = useState('2026-11-02')
  const [baseSalary, setBaseSalary] = useState('')
  const [bonus, setBonus] = useState('0.00')
  const [allowances, setAllowances] = useState('0.00')
  const [jobLevel, setJobLevel] = useState('')
  const [jobTitle, setJobTitle] = useState('')
  const [reason, setReason] = useState('')
  const [exceptionReason, setExceptionReason] = useState('')
  const [terminationDate, setTerminationDate] = useState('2026-11-02')

  function load() {
    api<EmployeeDetail>(`/api/employees/${id}`)
      .then((next) => {
        setDetail(next)
        setBaseSalary(next.employee.baseSalary ?? '')
        setBonus(next.employee.annualBonus ?? '0.00')
        setAllowances(next.employee.allowances ?? '0.00')
        setJobLevel(next.employee.jobLevel)
        setJobTitle(next.employee.jobTitle)
        setError(null)
      })
      .catch((caught) => setError(problem(caught)))
  }

  useEffect(() => { load() }, [id])

  async function submitChange(event: FormEvent) {
    event.preventDefault()
    setMessage(null)
    try {
      const next = await api<EmployeeDetail>(`/api/employees/${id}/compensation`, {
        method: 'POST',
        body: JSON.stringify({
          changeType,
          effectiveDate,
          baseSalary,
          annualBonus: bonus,
          allowances,
          jobLevel: changeType === 'PROMOTION' || changeType === 'CORRECTION' ? jobLevel : null,
          jobTitle: jobTitle || null,
          reason,
          exceptionReason: exceptionReason || null
        })
      })
      setDetail(next)
      setMessage('Pay change recorded.')
      setError(null)
    } catch (caught) {
      setError(problem(caught))
    }
  }

  async function terminate(event: FormEvent) {
    event.preventDefault()
    if (!window.confirm('Terminate this employee? Pay history is kept.')) return
    try {
      const next = await api<EmployeeDetail>(`/api/employees/${id}/terminate`, {
        method: 'POST',
        body: JSON.stringify({ terminationDate })
      })
      setDetail(next)
      setMessage('Employee terminated. History was kept.')
      setError(null)
    } catch (caught) {
      setError(problem(caught))
    }
  }

  if (!detail) return <><Banner error={error} /><p>Loading employee…</p></>
  const person = detail.employee
  return (
    <>
      <div className="page-head">
        <div>
          <h1>{person.firstName} {person.lastName}</h1>
          <p>{person.employeeNumber} · {person.jobTitle} · {person.departmentName} · {person.countryName}</p>
        </div>
        <span className={person.status === 'ACTIVE' ? 'pill good' : 'pill'}>{titleCase(person.status)}</span>
      </div>
      <Banner error={error} />
      {message && <div className="ok">{message}</div>}
      <div className="grid-2">
        <section className="card">
          <h2>Current annual pay</h2>
          <div className="stats">
            <div className="stat"><span>Base</span><strong>{money(person.baseSalary, person.currency)}</strong></div>
            <div className="stat"><span>USD base</span><strong>{usd(person.baseSalaryUsd)}</strong></div>
            <div className="stat"><span>Compa-ratio</span><strong>{compa(person.compaRatio)}</strong></div>
            <div className="stat"><span>Band</span><strong>{bandLabel(person.bandPosition)}</strong></div>
          </div>
          {detail.bandMin && detail.bandMax && person.baseSalary && person.currency && (
            <BandMeter min={detail.bandMin} max={detail.bandMax} value={person.baseSalary} currency={person.currency} />
          )}
          <p className="muted">Bonus {money(person.annualBonus, person.currency)} · allowances {money(person.allowances, person.currency)} · total USD {usd(person.totalCompUsd)}</p>
          <p className="muted">Hired {person.hireDate}{person.terminationDate ? ` · left ${person.terminationDate}` : ''}</p>
        </section>
        <section className="card">
          <h2>Record a change</h2>
          {person.status !== 'ACTIVE' ? <p>Terminated employees cannot receive a new pay row.</p> : (
            <form className="form-grid column" onSubmit={submitChange}>
              <label className="field">Change
                <select value={changeType} onChange={(event) => setChangeType(event.target.value)}>
                  {(meta?.changeTypes ?? ['MERIT', 'PROMOTION', 'ADJUSTMENT', 'CORRECTION']).map((type) => <option key={type}>{type}</option>)}
                </select>
              </label>
              <Text label="Effective date" value={effectiveDate} onChange={setEffectiveDate} type="date" />
              <Text label="Annual base" value={baseSalary} onChange={setBaseSalary} />
              <Text label="Annual bonus" value={bonus} onChange={setBonus} />
              <Text label="Annual allowances" value={allowances} onChange={setAllowances} />
              {(changeType === 'PROMOTION' || changeType === 'CORRECTION') && (
                <label className="field">New level
                  <select value={jobLevel} onChange={(event) => setJobLevel(event.target.value)}>
                    {meta?.levels.map((level) => <option key={level}>{level}</option>)}
                  </select>
                </label>
              )}
              <Text label="Job title" value={jobTitle} onChange={setJobTitle} />
              <Text label="Reason" value={reason} onChange={setReason} />
              <Text label="Exception reason" value={exceptionReason} onChange={setExceptionReason} />
              <button type="submit">Record change</button>
            </form>
          )}
        </section>
      </div>
      <section className="card">
        <h2>History</h2>
        <table>
          <thead><tr><th>Effective</th><th>Type</th><th>Level</th><th>Base</th><th>Band</th><th>Why</th></tr></thead>
          <tbody>
            {detail.history.map((row) => (
              <tr key={row.id}>
                <td>{row.effectiveDate}</td>
                <td>{titleCase(row.changeType)}</td>
                <td>{row.jobLevel}</td>
                <td>{money(row.baseSalary, row.currency)}</td>
                <td>{bandLabel(row.bandPosition)}</td>
                <td>{row.exceptionReason || row.reason || '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
      {person.status === 'ACTIVE' && (
        <form className="card filters" onSubmit={terminate}>
          <div>
            <h2>Termination</h2>
            <p className="muted">This keeps every pay row. It blocks later changes.</p>
          </div>
          <Text label="Termination date" value={terminationDate} onChange={setTerminationDate} type="date" />
          <button className="danger" type="submit">Terminate</button>
        </form>
      )}
    </>
  )
}

export function AuditPage() {
  const [page, setPage] = useState(0)
  const [data, setData] = useState<AuditPage | null>(null)
  const [error, setError] = useState<string[] | null>(null)
  useEffect(() => {
    api<AuditPage>(`/api/audit?page=${page}&size=25`)
      .then((next) => { setData(next); setError(null) })
      .catch((caught) => setError(problem(caught)))
  }, [page])
  const pages = data ? Math.max(1, Math.ceil(data.total / data.size)) : 1
  return (
    <>
      <div className="page-head">
        <div>
          <h1>Audit</h1>
          <p>Sign-ins and pay changes. Amounts stay in this log and out of the application log.</p>
        </div>
      </div>
      <Banner error={error} />
      <section className="card">
        {!data ? <p>Loading audit…</p> : (
          <table>
            <thead><tr><th>When</th><th>Who</th><th>Action</th><th>Summary</th></tr></thead>
            <tbody>
              {data.items.map((row) => (
                <tr key={row.id}>
                  <td>{row.occurredAt.replace('T', ' ').replace('Z', ' UTC')}</td>
                  <td>{row.actor}</td>
                  <td>{row.action}</td>
                  <td>{row.summary}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        <div className="pager">
          <span>Page {page + 1} of {pages}</span>
          <span>
            <button className="secondary" type="button" disabled={page <= 0} onClick={() => setPage(page - 1)}>Previous</button>{' '}
            <button className="secondary" type="button" disabled={page + 1 >= pages} onClick={() => setPage(page + 1)}>Next</button>
          </span>
        </div>
      </section>
    </>
  )
}

function Text({ label, value, onChange, type = 'text' }: { label: string; value: string; onChange: (value: string) => void; type?: string }) {
  return (
    <label className="field">{label}
      <input type={type} value={value} onChange={(event) => onChange(event.target.value)} />
    </label>
  )
}

function Select({ label, value, onChange, options }: { label: string; value: string; onChange: (value: string) => void; options: { code: string; label: string }[] }) {
  return (
    <label className="field">{label}
      <select value={value} onChange={(event) => onChange(event.target.value)}>
        {options.map((option) => <option key={option.code} value={option.code}>{option.label}</option>)}
      </select>
    </label>
  )
}
