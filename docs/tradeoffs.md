# Tradeoffs

## JDBC instead of JPA

Hibernate’s SQLite dialect is a community dialect, and money mapping is where ORMs get quiet bugs: floats, scale, and batch identity. The schema is small and stable. Explicit SQL is shorter to review than an ORM plus a dialect plus workarounds. The cost is hand-written row mapping. That cost is paid in the repository classes and covered by the API tests.

## Append-only history plus a current-pay projection

Computing “current salary” with a window function on every directory page is correct and, at 10,000 rows, still fast. A projection is faster to sort and makes the dashboard a straight read. The cost is another table to update in the same transaction. Future-dated rows make the projection necessary: the latest row is not always the row in force today.

## Rules in Java, not only in the database

CHECK constraints do not express “25% unless promotion” or “exception reason required when outside the band”. The policy class does. The database still enforces uniqueness, foreign keys, and non-null columns. A later SQL script can bypass the policy; the threat model treats anyone with the database file as already trusted with the data.

## Strings for money in JSON and in SQLite

SQLite has no exact decimal type. Storing canonical scale-2 strings avoids binary float error. The API uses the same representation so a client cannot send `0.1` as a binary number. Sorting and summing parse those strings back to `BigDecimal` in Java, or cast to REAL only for sort order. Sort order of two-decimal amounts in this range matches the cast. Sums used for reporting are `BigDecimal`, not REAL.

## In-process statistics

The dashboard loads the active workforce’s current pay and aggregates in `OrgPayCalculator`. Ten thousand decimals is a small heap object and the behavior is easy to test, including suppression and percentiles. Past roughly a hundred thousand employees this should move to SQL or a nightly summary. Doing that now would add approximate-median code without helping the stated scale.

## One SQLite file and one process

A server database would be the production choice for backups, concurrent HR users, and hosted deploy. It would also be extra setup for a reviewer. SQLite’s write lock is acceptable because pay changes are rare compared with reads, and the connection pool is one connection. Login throttling is therefore also process-local.

## Session cookie instead of a JWT in the browser

A token in `localStorage` is readable by any script that gets onto the page. An HTTP-only session cookie is not. CSRF then has to be handled, and it is, with a double-submit cookie. JWT would be justified for many services. This product is one service.

## Fixed departments, countries, and bands

Free-text department names would make the breakdown charts lie. The catalog is code and seed data. HR cannot fix a wrong band from the UI in this version. That is a product gap, accepted so band changes do not silently rewrite history. Each pay row copies the band it was judged against.

## Gender on the employee record

Pay equity is one of the questions an HR manager is expected to ask, and it cannot be answered without a demographic field. Showing individual gender beside salary to the same HR manager is part of the job. Publishing tiny groups is not. Suppression at five is a reporting control, not a claim that five is a universal privacy threshold.

## Compa-ratio on base salary

Including bonus would make sales and engineering incomparable and would mix a target with a salary. The dashboard therefore shows total compensation as its own number. The limitation is real: a low base with a large bonus can look like a band problem. The employee page shows both.

## Demo credential in configuration

The reviewer has to be able to sign in. The password lives in configuration and the login screen names it. That is a deliberate local-only choice, called out in `docs/security.md`, not an oversight.
