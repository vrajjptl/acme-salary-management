CREATE TABLE IF NOT EXISTS app_user (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  username TEXT NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  role TEXT NOT NULL,
  enabled INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS employee_number_seq (
  id INTEGER PRIMARY KEY CHECK (id = 1),
  next_value INTEGER NOT NULL
);

INSERT INTO employee_number_seq (id, next_value)
SELECT 1, 1
WHERE NOT EXISTS (SELECT 1 FROM employee_number_seq WHERE id = 1);

CREATE TABLE IF NOT EXISTS fx_rate (
  currency TEXT PRIMARY KEY,
  usd_per_unit TEXT NOT NULL,
  as_of_date TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS salary_band (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  country_code TEXT NOT NULL,
  job_level TEXT NOT NULL,
  currency TEXT NOT NULL,
  band_min TEXT NOT NULL,
  band_mid TEXT NOT NULL,
  band_max TEXT NOT NULL,
  UNIQUE (country_code, job_level)
);

CREATE TABLE IF NOT EXISTS employee (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  employee_number TEXT NOT NULL UNIQUE,
  first_name TEXT NOT NULL,
  last_name TEXT NOT NULL,
  email TEXT NOT NULL UNIQUE,
  country_code TEXT NOT NULL,
  department TEXT NOT NULL,
  job_title TEXT NOT NULL,
  job_level TEXT NOT NULL,
  status TEXT NOT NULL,
  gender TEXT NOT NULL,
  hire_date TEXT NOT NULL,
  termination_date TEXT
);

CREATE INDEX IF NOT EXISTS idx_employee_status ON employee(status);
CREATE INDEX IF NOT EXISTS idx_employee_country ON employee(country_code);
CREATE INDEX IF NOT EXISTS idx_employee_department ON employee(department);
CREATE INDEX IF NOT EXISTS idx_employee_level ON employee(job_level);
CREATE INDEX IF NOT EXISTS idx_employee_name ON employee(last_name, first_name);

CREATE TABLE IF NOT EXISTS compensation_record (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  employee_id INTEGER NOT NULL REFERENCES employee(id),
  effective_date TEXT NOT NULL,
  base_salary TEXT NOT NULL,
  currency TEXT NOT NULL,
  annual_bonus TEXT NOT NULL,
  allowances TEXT NOT NULL,
  change_type TEXT NOT NULL,
  job_level TEXT NOT NULL,
  reason TEXT,
  exception_reason TEXT,
  created_at TEXT NOT NULL,
  created_by TEXT NOT NULL,
  base_salary_usd TEXT NOT NULL,
  total_comp_usd TEXT NOT NULL,
  compa_ratio TEXT NOT NULL,
  band_min TEXT NOT NULL,
  band_mid TEXT NOT NULL,
  band_max TEXT NOT NULL,
  band_position TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_comp_employee ON compensation_record(employee_id, effective_date, id);

CREATE TABLE IF NOT EXISTS current_compensation (
  employee_id INTEGER PRIMARY KEY REFERENCES employee(id),
  compensation_id INTEGER NOT NULL REFERENCES compensation_record(id),
  effective_date TEXT NOT NULL,
  base_salary TEXT NOT NULL,
  currency TEXT NOT NULL,
  annual_bonus TEXT NOT NULL,
  allowances TEXT NOT NULL,
  base_salary_usd TEXT NOT NULL,
  total_comp_usd TEXT NOT NULL,
  compa_ratio TEXT NOT NULL,
  band_min TEXT NOT NULL,
  band_mid TEXT NOT NULL,
  band_max TEXT NOT NULL,
  band_position TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS audit_event (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  occurred_at TEXT NOT NULL,
  actor TEXT NOT NULL,
  action TEXT NOT NULL,
  entity_type TEXT NOT NULL,
  entity_id TEXT NOT NULL,
  summary TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_time ON audit_event(occurred_at);
