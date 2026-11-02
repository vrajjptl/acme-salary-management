# Performance

The target is an interactive HR tool at 10,000 employees, not a multi-tenant payroll platform.

## What is sized for 10,000

- Directory queries filter and page in SQLite. The default page is 25 and the maximum is 100, so a client cannot ask for the full table in one response.
- The list query joins `employee` to `current_compensation` once. There is no per-row follow-up query.
- Indexes cover status, country, department, level, name, employee number, and `(employee_id, effective_date, id)` on history.
- Dashboard math runs on the active current-pay set. The rows needed are a few numbers per person. That is well under a megabyte and finishes in well under a second. Aggregation rules live in `OrgPayCalculator` so this choice stays testable.
- The USD amount used for sorting is cast to REAL only inside `ORDER BY`. Filtering predicates use bound parameters.

## Seed

The generator builds the population in memory with a fixed random seed, checks every history against `CompensationPolicy`, then inserts employees, history, and the current projection in batched JDBC inside one transaction. A transaction is the main SQLite speed lever. The seed runs only when the employee table is empty, so a restart does not rewrite the file.

Generating 10,000 people is a straight loop of small objects. The slow part is the insert, and it is a startup cost, not a request cost.

## Write path

A pay change is a handful of indexed reads, one insert, one projection upsert, and one audit insert. That is the right shape for an event that happens per person a few times a year.

## Knobs that protect the process

- Hikari pool size is 1. SQLite allows one writer. A larger pool would spend time on `SQLITE_BUSY` instead of answering.
- `busy_timeout` is 5 seconds so a rare overlap waits instead of failing immediately.
- WAL mode is enabled for the file database so a reader is less likely to block behind a short write. The in-memory test database does not use WAL.
- Foreign keys are enabled on every connection.
- JSON bodies use Spring’s default size limit. Text fields are also capped in the domain so a huge reason cannot become a huge row.
- Page index is capped to stop a client from forcing a giant `OFFSET`.

## What was measured while building

The unit suite, including a 2,000-person generated population passed through the real policy, is the regression check for “the rules are not quadratic in a scary way”. The API test uses a small fixture. The 10,000-person seed is the manual scale check: start the application against an empty `data/` directory and confirm the process becomes ready and the directory still pages.

## When this design should change

Move aggregation into SQL or a summary table before 100,000 employees. Move off SQLite before more than one application instance. Add a job to roll future-dated rows into `current_compensation` if the process is expected to stay up across the effective date without a write; today, opening the employee or recording any change refreshes that employee, and startup does not scan for due future rows. At this scale, restarting the app the next morning is not the refresh strategy — the employee read path refreshes current pay for that person. The dashboard reads the projection, so a future-dated row that becomes due overnight appears after that employee is read or after the next write. For a demo that is acceptable. A daily refresh is the production follow-up and is noted here so it is not forgotten.
