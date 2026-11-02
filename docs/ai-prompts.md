# How AI was used

The assessment asks for AI to be used on purpose, and for the artifacts that show the thinking. This file is that record. It is not a claim that a human typed every line by hand, and it is not a fabricated prompt diary.

## Instruction that started the work

The human asked to complete the candidate brief in `Salary Management Assessment- Candidates.pdf` at a senior bar, including the edge cases, security, business rules, and the markdown a reviewer would expect, so the submission could be shortlisted.

The PDF itself requires:

- a one-page requirements document before the build, including what is left out
- a working backend and UI for an HR manager overseeing pay for 10,000 employees
- a relational database and a React UI
- a seed of 10,000 employees
- meaningful, fast, deterministic tests
- incremental commits and the notes used to make decisions

The stack constraint added by the human was Spring Boot for the API and React for the UI.

## Constraints given to the implementation

These were treated as binding, ahead of extra features:

- Write the requirements, rules, security notes, edge cases, architecture, tradeoffs, and performance notes so a reviewer can see the decisions without reading every class first.
- Keep the requirements document to a page. Put the detailed rules in their own document and make the code match them.
- Put compensation rules in a pure function with direct unit tests. Do not hide them inside a controller.
- Treat pay as confidential: no salary values in application logs, no password hash in the session, CSRF on a cookie session, parameterized SQL, an allowlist for sort columns, money as strings, equity groups suppressed under five people.
- Prefer a design that stays understandable at 10,000 employees over a design that anticipates a platform company.
- Leave payroll, tax, employee self-service, live FX, and approval chains out, and say why.
- Make the seed data pass the same policy as a pay change typed by HR, including the out-of-band cases, which must carry exception reasons.
- Commit in slices that follow the work: documents, then the service and tests, then the UI.

## What the model decided, and what it refused to invent

The PDF does not define bands, increase caps, or privacy rules. Those were chosen and written down: a 25% cap, append-only history, three-month future dating, and a minimum group size of five. They are product decisions, not facts from ACME.

No production HR system, real employee file, or real salary table was used. The workforce is synthetic. FX rates are a labeled snapshot.

The demo password is documented because a reviewer must sign in. It is not an attempt to hide a secret in the repository.

## What still belongs to a person

A reviewer will want a short screen recording. `docs/demo-script.md` is the path to record. Hosting an internet deployment was not done, because no target environment was provided. The run path is a local Spring Boot process, with a Docker file for anyone who wants the same process in a container.
