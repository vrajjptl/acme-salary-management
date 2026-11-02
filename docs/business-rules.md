# Compensation business rules

These rules are implemented in `CompensationPolicy` and the employee service. Tests in `CompensationPolicyTest` and `EmployeeApiTest` lock them in. If this document and the code disagree, the code and tests are the source of truth and this document should be updated in the same change.

Money is an annual amount with at most two decimal places, from `0.00` through `999999999.99`. Base salary must be greater than zero. Bonus and allowances may be zero and may not be negative. The API accepts money only as JSON strings so binary floating point never enters the request.

## Definitions

- **Total compensation** = annual base + annual bonus target + annual allowances, in the local currency.
- **Band** is the min, midpoint, and max for a country and job level. Comparison to band uses **base only**.
- **Compa-ratio** = base ÷ band midpoint, stored at four decimal places.
- **Band position** is `BELOW`, `IN_BAND`, or `ABOVE`.
- **Current pay** is the compensation row with the latest effective date on or before today (UTC). A tie uses the higher row id. Future-dated rows stay in history and do not become current until that date.
- **USD figures** use the snapshot table `fx_rate` (`local × usd_per_unit`), rounded half up to cents. The snapshot date is stored with the rate. It is not a live market feed.
- Dashboard statistics include **active employees who already have current pay**. Former employees stay on file and on their own page. People with only a future-dated hire are counted separately as scheduled, not in payroll.

## Level ladder

Ranks increase in this order: L1, L2, L3, L4, L5, L6, M1, M2, M3. Manager midpoints sit above L6 so a promotion does not imply a pay cut. A promotion must move up, and by at most two ranks. A correction may set any level because it fixes bad data.

## What each change type may do

| Type | When | Level | Notes |
| --- | --- | --- | --- |
| HIRE | First row only, and only by creating the employee | Already chosen on the employee | Effective date must equal the hire date |
| MERIT | Later row, same level | Cannot change | Subject to the 25% cap |
| ADJUSTMENT | Later row, same level | Cannot change | Same 25% cap, so the label cannot bypass the cap |
| PROMOTION | Later row | Up 1 or 2 ranks | The 25% cap does not apply. The new band does |
| CORRECTION | Later row, including the same effective date | Any level | The only way to record a second change on a date that already has a row |

There is no update and no delete of a compensation row.

## Rules enforced on every new row

1. The employee must be active.
2. Effective date is required, is a real calendar date, is on or after the hire date, is on or before the termination date when one exists, and is not more than three months after today.
3. Effective date cannot be earlier than the latest row. If it equals the latest row’s date, the change type must be CORRECTION.
4. A reason of 8 to 500 characters is required except on HIRE, where it is optional.
5. If base is below the band minimum or above the maximum, an exception reason of 12 to 500 characters is required.
6. If base rises by more than 25.00% versus current base, an exception reason is required, except on PROMOTION.
7. A decrease versus current base always requires an exception reason, including on promotion.
8. An exception reason that is present but shorter than 12 characters is rejected even when no exception is required, so the file does not collect meaningless notes.
9. Currency is taken from the band for the target country and level. The client does not choose a currency.
10. Hire, merit, adjustment, promotion, and correction rows that fail these rules are not stored. The database is not used as the place where bad pay is cleaned up later.

Termination is separate from pay. The date must fall on or after the hire date and on or before today, and no compensation row may be effective after it. A second termination is rejected. Termination does not delete history.

Profile edits may change first name, last name, job title, department, and gender. They may not change country, hire date, email, level, or pay. Level moves only through promotion or correction. Email is stored in lowercase and is unique. Employee numbers are allocated by the server as `E` plus six digits.

## Reporting rules

- Percentiles use the nearest-rank method on base USD: rank = ceil(p × n).
- Median of an even count is the mean of the two central values, at cent precision.
- A gender group with fewer than five active people in the current filter is omitted. The response reports how many groups were suppressed. This applies after a country or department filter, so a small slice cannot re-identify someone.
- Equity uses base USD, for the same reason compa-ratio uses base.

## Reference data

Ten countries are supported: US, GB, DE, IN, SG, BR, AE, CA, AU, NL. Departments are a fixed list so breakdowns stay meaningful. Adding a country means adding a currency, an FX row, and a band for every level. That is data work, not a free-text field.
