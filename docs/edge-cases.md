# Edge cases

Each row is a case the HR manager or a hostile client can actually hit. “Where” points at the automated test or the type that enforces it.

| Case | Expected behavior | Where |
| --- | --- | --- |
| Missing session on an API call | 401 JSON, no redirect to an HTML login | `EmployeeApiTest.apiRejectsAnonymousCalls` |
| Unknown user and wrong password | Same 401 message, no password in the audit row | `EmployeeApiTest.badLoginDoesNotRevealWhichPartFailed` |
| Eight failed logins from one IP | 429, and the correct password is not checked while the block holds | `EmployeeApiTest.loginLocksOutAfterRepeatedFailures`, `LoginThrottleTest` |
| JSON money sent as a number | 400, value not stored | `EmployeeApiTest.moneyMustBeAJsonString` |
| More than two decimal places, a leading minus, or scientific notation | Field error | `MoneyTextTest` |
| Base of zero or a negative bonus | Field error | `MoneyTextTest` |
| Salary above the band with no exception reason | 422, nothing stored | `CompensationPolicyTest`, `EmployeeApiTest` |
| Salary above the band with a real exception reason | Stored, band position `ABOVE` | `CompensationPolicyTest.salaryOutsideTheBandNeedsAnException` |
| Merit increase of exactly 25% | Allowed | `CompensationPolicyTest.meritAtExactlyTwentyFivePercentIsAllowed` |
| Merit increase above 25% | Rejected until an exception reason is present | `CompensationPolicyTest` |
| Adjustment relabeled to dodge the cap | Same cap as merit | `CompensationPolicyTest.adjustmentCannotBypassTheIncreaseCap` |
| Promotion above 25% inside the new band | Allowed | `CompensationPolicyTest.promotionMayExceedTheCapInsideTheNewBand` |
| Promotion that jumps more than two levels | Rejected | `CompensationPolicyTest` |
| Promotion to the same or a lower level | Rejected | `CompensationPolicyTest` |
| Decrease without an exception reason | Rejected, including on a promotion | `CompensationPolicyTest` |
| Second change on the same effective date | Only a correction is accepted | `CompensationPolicyTest` |
| Effective date before the latest row | Rejected. History stays ordered | `CompensationPolicyTest` |
| Effective date before hire, or more than three months out | Rejected | `CompensationPolicyTest` |
| Hire effective date different from the hire date | Rejected | `CompensationPolicyTest` |
| Second hire row | Rejected | `CompensationPolicyTest` |
| Change for a terminated employee | Rejected | `EmployeeApiTest` |
| Termination in the future, before hire, or while a later pay row exists | Rejected | `EmployeeApiTest` |
| Second termination | Rejected | `EmployeeApiTest` |
| Future-dated increase | Stored in history, current pay unchanged | `EmployeeApiTest.futureDatedIncreaseDoesNotReplaceCurrentPay` |
| Two rows, same date, correction later | The higher id wins as current | `CompensationRepository` ordering, covered by the future-date and correction flows |
| Unknown sort field or sort text that looks like SQL | 400, query uses the allowlist only | `EmployeeSortTest`, `EmployeeApiTest` |
| Page size above 100 or a negative page | 400 | `PagesTest` |
| Search containing quotes, `%`, or a tautology | Treated as literal text, not as SQL | `EmployeeApiTest.searchDoesNotTreatTheQueryAsSql` |
| Duplicate email | 409 | `EmployeeApiTest` |
| Unknown JSON property on create | 400 | `EmployeeApiTest` |
| Gender group smaller than five | Omitted from equity, suppression count increased | `OrgPayCalculatorTest` |
| Filter that makes a previously large group small | Suppression runs after the filter | `OrgPayCalculatorTest` |
| No active pay rows | Dashboard returns zeros and null averages, not a 500 | `OrgPayCalculatorTest.emptyWorkforce` |
| Even number of salaries | Median is the mean of the two middle values | `OrgPayCalculatorTest` |
| Bonus outside the band while base is inside | Band position stays in band. Compa-ratio uses base | `PaySnapshotTest` |
| Unsupported country or department | Field error. Free text cannot fragment reporting | `EmployeeApiTest` |
| Control characters or angle brackets in a name | Field error | `TextRulesTest` |
| Delete compensation or employee | No route. A DELETE on the employee URL is rejected | `EmployeeApiTest` |
| Seeded population | Every generated history passes the same policy, including intentional out-of-band rows that carry an exception reason | `SeedFactoryTest` |
| Restart with an existing database | Seed does not insert another 10,000 people | `SeedRunner` counts employees first |
| SQLite file locked by a second writer | Pool size is one, busy timeout is set, seed runs in a single transaction | `SqliteConfig`, `docs/performance.md` |

Cases considered and intentionally not automated: a stolen session cookie, a crashed disk mid-commit (SQLite’s transaction is the mitigation), and a malicious HR manager. The manager is allowed to see every salary. The product’s job is to make that access attributable, not to hide the workforce from HR.
