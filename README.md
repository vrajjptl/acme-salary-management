# ACME Compensation

Web workspace for an HR manager who currently keeps salary data for about 10,000 employees in spreadsheets. It records pay history, enforces compensation rules, and answers how the organization pays people.

The one-page brief is `docs/requirements.md`. Rules, security, edge cases, architecture, tradeoffs, and performance notes are in `docs/`.

## Demo sign-in

`hr.manager` / `Hr-Manager-Demo-2026!`

That password is local fixture data. Do not expose this process on a network you do not control without replacing it. There is no registration page.

## Run it

Requirements: Java 17, Maven, Node 22.

```powershell
cd frontend
npm install
npm run dev
```

In a second terminal, from `backend`:

```powershell
mvn spring-boot:run
```

Open http://localhost:5173. The first launch writes `backend/data/acme-salary.db` and seeds 10,000 employees. A later launch keeps that file.

Tests:

```powershell
cd backend
mvn test
```

One process, after building the UI into the API:

```powershell
cd frontend
npm run build
cd ../backend
mvn -DskipTests package
cd ..
java -jar backend/target/acme-compensation.jar
```

Then open http://localhost:8080. The SQLite file is created in `./data` under the directory you started Java from.

`docs/demo-script.md` is the short path for a screen recording.

## What the HR manager can do

- Read payroll, median and mean base, percentiles, and band compliance for active employees.
- Slice that picture by country or department.
- See gender pay only when a group has at least five people.
- Search and page the directory.
- Add a person, append a merit, promotion, adjustment, or correction, and terminate someone without deleting history.

Pay is stored in local currency. Reporting amounts use a dated FX snapshot, not a live feed. Compa-ratio uses annual base salary.
