export interface User {
  username: string
  role: string
}

export interface Coded {
  code: string
  label: string
}

export interface Meta {
  countries: Coded[]
  departments: Coded[]
  levels: string[]
  changeTypes: string[]
  genders: Coded[]
}

export interface EmployeeItem {
  id: number
  employeeNumber: string
  firstName: string
  lastName: string
  email: string
  country: string
  countryName: string
  department: string
  departmentName: string
  jobTitle: string
  jobLevel: string
  status: string
  gender: string
  genderName: string
  hireDate: string
  terminationDate: string | null
  currency: string | null
  baseSalary: string | null
  annualBonus: string | null
  allowances: string | null
  baseSalaryUsd: string | null
  totalCompUsd: string | null
  compaRatio: string | null
  bandPosition: string | null
}

export interface EmployeePage {
  page: number
  size: number
  total: number
  items: EmployeeItem[]
}

export interface HistoryItem {
  id: number
  effectiveDate: string
  baseSalary: string
  currency: string
  annualBonus: string
  allowances: string
  changeType: string
  jobLevel: string
  reason: string | null
  exceptionReason: string | null
  createdAt: string
  createdBy: string
  baseSalaryUsd: string
  compaRatio: string
  bandPosition: string
}

export interface EmployeeDetail {
  employee: EmployeeItem
  bandMin: string | null
  bandMid: string | null
  bandMax: string | null
  history: HistoryItem[]
}

export interface Breakdown {
  key: string
  label: string
  headcount: number
  totalBaseUsd: string
  medianBaseUsd: string | null
  meanBaseUsd: string | null
  belowBand: number
  inBand: number
  aboveBand: number
}

export interface EquityRow {
  group: string
  label: string
  headcount: number
  medianBaseUsd: string | null
  meanBaseUsd: string | null
}

export interface PaySummary {
  headcount: number
  totalBaseUsd: string
  totalCompUsd: string
  medianBaseUsd: string | null
  meanBaseUsd: string | null
  p25BaseUsd: string | null
  p75BaseUsd: string | null
  belowBand: number
  inBand: number
  aboveBand: number
  outsideBandPercent: string
  byCountry: Breakdown[]
  byDepartment: Breakdown[]
  byLevel: Breakdown[]
  equity: EquityRow[]
  suppressedGroups: number
  minimumGroupSize: number
}

export interface Analytics {
  activeWithPay: number
  formerHeadcount: number
  scheduledHeadcount: number
  pay: PaySummary
  fxAsOf: string | null
  country: string | null
  department: string | null
  notes: string[]
}

export interface AuditPage {
  page: number
  size: number
  total: number
  items: {
    id: number
    occurredAt: string
    actor: string
    action: string
    entityType: string
    entityId: string
    summary: string
  }[]
}

export interface ApiPayload {
  error?: string
  message?: string
  violations?: string[]
  fields?: Record<string, string>
}
