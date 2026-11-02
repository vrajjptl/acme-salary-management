# ACME Compensation — requirements

## Goal

Give ACME’s HR manager one system of record for how the organization pays about 10,000 employees across countries, instead of a set of spreadsheets. The manager must be able to find a person, see current pay against the band, record a pay change without destroying history, and answer “how do we pay people?” by country, department, and level.

## Users and scope

The only user is the HR manager. The product is an internal compensation workspace, not a payroll engine and not an employee self-service portal.

In scope:

- Sign in as the HR manager, with session expiry and a lockout on repeated failures.
- Search and filter the workforce. Review current annual pay, band position, and compa-ratio (base ÷ band midpoint).
- Add an employee with an initial hire salary.
- Append a merit increase, promotion, adjustment, or correction. History is never edited in place or deleted.
- Correct non-pay profile data (name, title, department, gender) and record a termination.
- Dashboard of active-employee pay: headcount, annual base payroll, annual total compensation, median and mean base, 25th and 75th percentile, band compliance, and breakdowns by country, department, and level.
- Gender pay comparison that hides groups smaller than five people.
- Amounts stored in local currency and reported in USD using a dated FX snapshot.
- A deterministic seed of 10,000 employees so the product can be judged at the stated scale.

## Deliberately out of scope

- Payroll calculation, tax, payslips, bank details, and payment files. Those are a different control environment.
- Benefits, equity grants, offer letters, and approval workflows. A written exception reason is enough accountability for this version.
- Employee login, manager hierarchies, and multi-company tenancy. One organization, one HR role.
- International transfers. Country and pay currency stay fixed; the band catalog would otherwise need a transfer policy.
- Live FX feeds and HRIS integrations. Rates are a visible snapshot so results are explainable.
- Editing salary bands in the UI. Bands are reference data, snapshotted onto each pay record.
- Variable-pay-aware bands. Compa-ratio uses base salary. Total compensation is shown beside it so bonus-heavy jobs are not mistaken for base pay.

## Success criteria

An HR manager can sign in, read the org-level pay picture, open a person, and record a valid change. Invalid changes (terminated employee, cut without a reason, increase above the cap, pay outside the band, backdated history) are rejected with a specific message. The same rules are covered by fast unit tests. Lists stay responsive at 10,000 employees because they are paged and filtered in the database.
