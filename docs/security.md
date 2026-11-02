# Security

Compensation data is confidential even when every row is fictional. The controls below are the ones this assessment system actually implements. They are not a claim of a completed enterprise security program.

## Assets

| Asset | Handling |
| --- | --- |
| Base, bonus, allowances, compa-ratio, exception reasons | Shown only to an authenticated HR manager. `Cache-Control: no-store` on every `/api` response |
| Gender | Collected for pay-equity reporting. Groups under five people are suppressed. Profile changes are audited |
| Passwords | BCrypt. The demo password is intentional local fixture data, set in `app.demo.password`. Remove that property before any real deployment. There is no registration endpoint |
| Session | HTTP-only `ACME_SESSION` cookie, SameSite=Lax, 30-minute idle timeout. The object stored in the session is an `HrPrincipal` whose password field is empty, so the BCrypt hash is not copied into the session |
| Audit trail | Authenticated read. Contains amounts and reasons because HR has to reconstruct a change. Application logs record ids, change type, and actor, not amounts, free text, or passwords |

## Authentication and authorization

- Every `/api/**` route except `POST /api/auth/login` and `GET /api/health` requires `ROLE_HR_MANAGER`.
- Anonymous authentication is disabled so a missing session becomes 401, not a confusing 403.
- Failed and successful sign-ins are audited. The failure record does not store the attempted username.
- The login response for a bad password and an unknown user is the same message. Spring Security’s default hidden user-not-found behavior avoids a user-enumeration oracle.
- `LoginThrottle` blocks an IP after 8 failures inside 5 minutes and answers 429 without checking the password again. The limit is per process and uses the direct client address. `X-Forwarded-For` is not trusted, because trusting it without a configured proxy would let a caller reset the key.
- There is one role because the brief has one persona. A future employee role would need row-level scope. This version does not add a half-built employee login.

## Request integrity

- Cookie-based CSRF token (`XSRF-TOKEN` readable by the UI, sent back as `X-XSRF-TOKEN`). State-changing requests without it fail.
- Login replaces any existing session id before the new session is created.
- JSON bodies that contain unexpected properties are rejected.
- Money must be a JSON string. A JSON number is rejected so floating point cannot arrive as a salary.
- Page size is capped at 100. Page index is capped. Sort columns and direction come from an allowlist; the user string is never concatenated into SQL.
- Search text is bound as a parameter. `%`, `_`, and `\` are stripped so a search cannot widen into “match everything”.
- Text fields reject control characters and `<` `>` and have length limits.
- Enum fields reject unknown codes.
- SQLite foreign keys are on. Compensation rows reference the employee. The API exposes no delete.

## Headers and browser

- Content-Security-Policy: `default-src 'self'; frame-ancestors 'none'; form-action 'self'; base-uri 'self'`.
- Frame denial, `nosniff`, and a no-referrer policy are enabled.
- The UI does not render HTML from employee fields. Values go through React text nodes.
- HSTS is not set on this HTTP demo. Turn it on only behind HTTPS, or browsers will cache it for localhost.
- CORS allows `http://localhost:5173` with credentials so the Vite dev server can call the API. The shipped UI is served by the same Spring Boot origin and does not need cross-origin access.

## Data integrity as a security property

History is append-only. A correction adds a row; it does not rewrite the earlier one. Current pay is a projection that can be rebuilt from history. That limits the damage of a buggy screen and gives the audit log a stable story.

## Operational limits

- The demo account is widely known inside this repository on purpose. Do not expose the process to a network you do not control without replacing the credential and putting TLS in front.
- Login throttling and sessions are in-memory. A second instance would not share them. One process matches SQLite, which is the chosen database.
- JDBC debug logging of parameters is left off. Turning on SQL parameter logs would write salaries to disk.
- Dependencies are Spring Boot’s managed set plus the SQLite JDBC driver. No extra HTTP client and no dynamic script loading in the UI.

## Threats this does not close

A stolen unlocked laptop session is still a stolen session. There is no step-up authentication, no device management, and no SIEM. Those belong to the company around the product, and pretending otherwise would be the weaker submission.
